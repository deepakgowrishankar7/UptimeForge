import { api } from './api.js';
import { auth } from './auth.js';
import { renderLayout, showToast, getStatusBadge, formatDate, formatDuration, escapeHtml, openModal, closeModal } from './common.js';

auth.requireAuth();
renderLayout('Dashboard');

let chartInstance = null;

async function loadDashboard() {
  try {
    const data = await api.get('/analytics/overview');

    // Populate Metrics
    const downCount = data.monitorsDown || 0;
    const degradedCount = data.monitorsDegraded || 0;
    const totalChecks = data.totalChecksCount || 0;
    document.getElementById('metric-total').innerText = data.totalMonitors || 0;
    document.getElementById('metric-breakdown').innerHTML = `
      <span style="color:#16A34A; font-weight:600;">${data.monitorsUp || 0} UP</span> · 
      <span style="color:${downCount > 0 ? '#DC2626' : 'inherit'}; font-weight:${downCount > 0 ? '700' : 'normal'};">${downCount} DOWN</span> · 
      <span style="color:${degradedCount > 0 ? '#D97706' : 'inherit'}; font-weight:${degradedCount > 0 ? '700' : 'normal'};">${degradedCount} ISSUES</span>
    `;
    document.getElementById('metric-uptime').innerText = totalChecks > 0 ? `${data.overallUptimePercent}%` : '0%';
    document.getElementById('metric-latency').innerText = totalChecks > 0 ? `${data.avgResponseTimeMs} ms` : '0 ms';
    document.getElementById('metric-incidents').innerText = data.activeIncidentsCount;
    document.getElementById('metric-incidents-sub').innerText = (data.activeIncidentsCount > 0 || downCount > 0) ? 'Outage detected — Attention required' : 'All systems operational';

    // Render Incidents Table
    const incTbody = document.getElementById('incidents-tbody');
    if (!data.recentIncidents || data.recentIncidents.length === 0) {
      incTbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:var(--text-dim); padding:24px;">No active incidents reported. All systems healthy.</td></tr>';
    } else {
      incTbody.innerHTML = data.recentIncidents.map(inc => `
        <tr>
          <td><a href="/monitor-details.html?id=${inc.monitorId}" style="font-weight:600;">${escapeHtml(inc.monitorName)}</a></td>
          <td>${getStatusBadge(inc.status)}</td>
          <td>${formatDate(inc.startedAt)}</td>
          <td>${formatDuration(inc.durationSeconds)}</td>
        </tr>
      `).join('');
    }

    // Render Monitors Table with Check Now Action
    const monTbody = document.getElementById('monitors-tbody');
    if (!data.topMonitors || data.topMonitors.length === 0) {
      monTbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color:var(--text-dim); padding:24px;">No monitors configured yet. Click "+ Add Monitor" to create one.</td></tr>';
    } else {
      monTbody.innerHTML = data.topMonitors.map(m => `
        <tr>
          <td>
            <a href="/monitor-details.html?id=${m.id}" style="font-weight:600; display:block;">${escapeHtml(m.name)}</a>
            <span style="font-size:12px; color:var(--text-dim);">${escapeHtml(m.url)}</span>
          </td>
          <td>${getStatusBadge(m.currentStatus)}</td>
          <td><strong style="color:${m.uptime24h > 99 ? 'var(--color-up)' : 'var(--color-degraded)'};">${m.uptime24h !== null ? m.uptime24h + '%' : 'N/A'}</strong></td>
          <td>${m.lastResponseTimeMs !== null ? m.lastResponseTimeMs + ' ms' : 'Pending'}</td>
          <td style="text-align:right;">
            <button class="btn btn-secondary check-now-btn" data-id="${m.id}" style="padding:4px 10px; font-size:12px; border-radius:6px; cursor:pointer;" title="Run Instant Health Probe">
              Check Now
            </button>
          </td>
        </tr>
      `).join('');

      // Attach event listeners for Check Now
      monTbody.querySelectorAll('.check-now-btn').forEach(btn => {
        btn.addEventListener('click', async (e) => {
          e.preventDefault();
          const id = btn.getAttribute('data-id');
          btn.disabled = true;
          btn.innerText = 'Checking...';
          try {
            const res = await api.post(`/monitors/${id}/test-check`);
            if (res.success) {
              showToast(`Probe Succeeded: HTTP ${res.statusCode || 200} (${res.responseTimeMs}ms)`, 'success');
            } else {
              showToast(`Probe Failed: ${res.errorType || 'ERROR'} (${res.errorMessage || 'Offline'})`, 'error');
            }
            await loadDashboard();
          } catch (err) {
            showToast(err.message || 'Check failed', 'error');
          } finally {
            btn.disabled = false;
            btn.innerText = 'Check Now';
          }
        });
      });
    }

    // Render Recent Failures & Diagnostic Stream
    const failTbody = document.getElementById('failures-tbody');
    if (failTbody) {
      if (!data.recentFailures || data.recentFailures.length === 0) {
        failTbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color:var(--text-dim); padding:24px;">No failed checks recorded. All endpoints operational!</td></tr>';
      } else {
        failTbody.innerHTML = data.recentFailures.map(f => `
          <tr>
            <td style="font-size:12px; color:var(--text-muted);">${formatDate(f.checkedAt)}</td>
            <td>
              ${f.statusCode 
                ? `<span style="font-family:monospace; font-weight:700; color:#DC2626; background:#FEE2E2; padding:2px 8px; border-radius:4px;">${f.statusCode}</span>` 
                : '<span style="font-size:11px; color:#DC2626; font-weight:600; background:#FEE2E2; padding:2px 6px; border-radius:4px;">NO_RESPONSE</span>'}
            </td>
            <td style="font-weight:600;">${f.responseTimeMs} ms</td>
            <td><strong style="color:#DC2626;">${escapeHtml(f.errorType || 'CONNECTION_FAILURE')}</strong></td>
            <td style="font-size:12px; color:#B91C1C; max-width:340px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;" title="${escapeHtml(f.errorMessage || '')}">
              ${escapeHtml(f.errorMessage || 'Connection failure / host unreachable')}
            </td>
          </tr>
        `).join('');
      }
    }

    // Load and Render Latency Chart
    loadLatencyChart();

  } catch (err) {
    showToast(err.message || 'Failed to load dashboard data', 'error');
  }
}

function formatDashboardTime(timestampStr) {
  if (!timestampStr) return '';
  try {
    const d = new Date(timestampStr);
    if (!isNaN(d.getTime())) {
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
    }
  } catch (e) {}
  if (typeof timestampStr === 'string' && timestampStr.includes(' ')) {
    const parts = timestampStr.split(' ');
    const timePart = parts[1] || parts[0];
    const sub = timePart.split(':');
    return sub.length >= 2 ? `${sub[0]}:${sub[1]}` : timestampStr;
  }
  return timestampStr;
}

async function loadLatencyChart() {
  try {
    const res = await api.get('/analytics/response-times', { days: 1 });
    const canvas = document.getElementById('latencyChart');
    if (!canvas) return;
    const parent = canvas.parentElement;

    if (chartInstance) {
      chartInstance.destroy();
      chartInstance = null;
    }

    const hasData = res.trend && res.trend.length > 0;
    if (!hasData) {
      canvas.style.display = 'none';
      let emptyMsg = parent.querySelector('.empty-dash-latency-msg');
      if (!emptyMsg) {
        emptyMsg = document.createElement('div');
        emptyMsg.className = 'empty-dash-latency-msg';
        emptyMsg.style.cssText = 'display:flex; flex-direction:column; align-items:center; justify-content:center; height:100%; color:var(--text-muted); gap:8px;';
        parent.appendChild(emptyMsg);
      }
      emptyMsg.innerHTML = `
        <svg viewBox="0 0 24 24" width="36" height="36" stroke="currentColor" stroke-width="1.5" fill="none" style="opacity:0.4;"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
        <span style="font-weight:600; font-size:14px; color:var(--text-main, #334155);">No Response Time Metrics Recorded</span>
        <span style="font-size:12px; color:var(--text-dim, #64748B);">Real-time latency graph will appear as monitoring probes run.</span>
      `;
      emptyMsg.style.display = 'flex';
      return;
    }

    const existingMsg = parent.querySelector('.empty-dash-latency-msg');
    if (existingMsg) existingMsg.style.display = 'none';
    canvas.style.display = 'block';

    const ctx = canvas.getContext('2d');
    const rawLabels = res.trend.map(p => p.timestamp);
    const formattedLabels = rawLabels.map(t => formatDashboardTime(t));
    const values = res.trend.map(p => p.latencyMs);

    const gradient = ctx.createLinearGradient(0, 0, 0, 240);
    gradient.addColorStop(0, 'rgba(37, 99, 235, 0.22)');
    gradient.addColorStop(1, 'rgba(37, 99, 235, 0.00)');

    if (window.Chart) {
      chartInstance = new Chart(ctx, {
        type: 'line',
        data: {
          labels: formattedLabels,
          datasets: [{
            label: 'Response Time (ms)',
            data: values,
            borderColor: '#2563EB',
            backgroundColor: gradient,
            fill: true,
            tension: 0.38,
            borderWidth: 2.5,
            pointRadius: values.length > 25 ? 0 : 3,
            pointHoverRadius: 6,
            pointHoverBackgroundColor: '#2563EB',
            pointHoverBorderColor: '#FFFFFF',
            pointHoverBorderWidth: 2
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            x: {
              grid: { display: false },
              ticks: {
                color: '#64748B',
                font: { size: 11, weight: '600' },
                maxTicksLimit: 8,
                autoSkip: true,
                maxRotation: 0,
                minRotation: 0
              }
            },
            y: {
              beginAtZero: true,
              grace: '20%',
              grid: { color: 'rgba(226, 232, 240, 0.7)' },
              ticks: {
                color: '#64748B',
                font: { size: 11, weight: '500' },
                callback: (val) => `${val} ms`
              }
            }
          },
          plugins: {
            legend: { display: false },
            tooltip: {
              backgroundColor: '#0F172A',
              titleColor: '#F8FAFC',
              bodyColor: '#38BDF8',
              titleFont: { size: 12, weight: '600' },
              bodyFont: { size: 13, weight: '700' },
              padding: { top: 8, bottom: 8, left: 12, right: 12 },
              cornerRadius: 8,
              displayColors: false,
              callbacks: {
                title: (items) => {
                  const idx = items[0].dataIndex;
                  return rawLabels[idx] || items[0].label;
                },
                label: (ctx) => `Response Time: ${ctx.parsed.y} ms`
              }
            }
          }
        }
      });
    }
  } catch (e) {
    console.warn('Latency chart rendering skipped:', e);
  }
}

// Modal handlers
const modal = document.getElementById('create-modal');
document.getElementById('create-monitor-btn')?.addEventListener('click', () => openModal('create-modal'));
document.getElementById('close-modal-btn')?.addEventListener('click', () => closeModal('create-modal'));
document.getElementById('cancel-modal-btn')?.addEventListener('click', () => closeModal('create-modal'));

document.getElementById('create-monitor-form')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const saveBtn = document.getElementById('save-monitor-btn');
  saveBtn.disabled = true;
  saveBtn.innerText = 'Creating...';

  const thresholdElem = document.getElementById('m-threshold');
  const payload = {
    name: document.getElementById('m-name').value.trim(),
    type: document.getElementById('m-type').value,
    httpMethod: document.getElementById('m-method').value,
    url: document.getElementById('m-url').value.trim(),
    intervalSeconds: parseInt(document.getElementById('m-interval').value),
    timeoutMs: parseInt(document.getElementById('m-timeout').value),
    failureThreshold: thresholdElem ? parseInt(thresholdElem.value) : 1,
    recoveryThreshold: 1,
    expectedStatusCodes: document.getElementById('m-status').value.trim()
  };

  try {
    await api.post('/monitors', payload);
    showToast('Health monitor created successfully!', 'success');
    closeModal('create-modal');
    document.getElementById('create-monitor-form').reset();
    loadDashboard();
  } catch (err) {
    showToast(err.message || 'Failed to create monitor', 'error');
  } finally {
    saveBtn.disabled = false;
    saveBtn.innerText = 'Create Monitor';
  }
});

// Ad-hoc Probe Web Modal Handlers (Replaces browser alert/prompt)
document.getElementById('quick-check-btn')?.addEventListener('click', () => {
  const resultContainer = document.getElementById('adhoc-result-container');
  if (resultContainer) resultContainer.style.display = 'none';
  openModal('adhoc-modal');
  document.getElementById('adhoc-url')?.focus();
});

document.getElementById('close-adhoc-btn')?.addEventListener('click', () => closeModal('adhoc-modal'));
document.getElementById('cancel-adhoc-btn')?.addEventListener('click', () => closeModal('adhoc-modal'));

document.getElementById('adhoc-probe-form')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const submitBtn = document.getElementById('run-adhoc-submit-btn');
  const resultContainer = document.getElementById('adhoc-result-container');
  const badgeElem = document.getElementById('adhoc-result-badge');
  const timeElem = document.getElementById('adhoc-result-time');
  const detailsElem = document.getElementById('adhoc-result-details');

  const url = document.getElementById('adhoc-url').value.trim();
  const type = document.getElementById('adhoc-type').value;
  const timeoutMs = parseInt(document.getElementById('adhoc-timeout').value) || 10000;

  submitBtn.disabled = true;
  submitBtn.innerText = 'Testing Endpoint...';
  resultContainer.style.display = 'none';

  try {
    const res = await api.post('/monitors/test', { url, type, timeoutMs });
    resultContainer.style.display = 'block';

    if (res.success) {
      badgeElem.className = 'badge badge-up';
      badgeElem.innerHTML = '<span class="badge-dot"></span>PROBE SUCCESSFUL';
      timeElem.innerText = `${res.responseTimeMs} ms`;
      detailsElem.innerHTML = `
        <div style="margin-bottom:6px;"><strong>HTTP Status:</strong> <span style="font-family:monospace; font-weight:700; color:#16A34A; background:#DCFCE7; padding:2px 8px; border-radius:4px;">${res.statusCode || 200} OK</span></div>
        <div style="color:var(--text-muted); font-size:12px;">Endpoint responded normally within the expected latency threshold.</div>
      `;
      showToast(`Probe Succeeded (${res.responseTimeMs}ms)`, 'success');
    } else {
      badgeElem.className = 'badge badge-down';
      badgeElem.innerHTML = '<span class="badge-dot"></span>PROBE FAILED';
      timeElem.innerText = res.responseTimeMs ? `${res.responseTimeMs} ms` : 'No response';
      detailsElem.innerHTML = `
        <div style="margin-bottom:6px;"><strong>Error Classification:</strong> <span style="font-family:monospace; font-weight:700; color:#DC2626; background:#FEE2E2; padding:2px 8px; border-radius:4px;">${escapeHtml(res.errorType || 'ERROR')}</span></div>
        <div style="margin-bottom:6px;"><strong>Diagnostic Message:</strong> <span style="color:#B91C1C; font-weight:600;">${escapeHtml(res.errorMessage || 'Target server did not respond')}</span></div>
        ${res.statusCode ? `<div style="font-size:12px; color:#DC2626;"><strong>Status Code Received:</strong> ${res.statusCode}</div>` : ''}
      `;
      showToast(`Probe Failed: ${res.errorType || 'ERROR'}`, 'error');
    }
  } catch (err) {
    resultContainer.style.display = 'block';
    badgeElem.className = 'badge badge-down';
    badgeElem.innerHTML = '<span class="badge-dot"></span>REQUEST ERROR';
    timeElem.innerText = 'Error';
    detailsElem.innerText = err.message || 'Failed to execute probe request';
    showToast(err.message || 'Execution error', 'error');
  } finally {
    submitBtn.disabled = false;
    submitBtn.innerText = 'Run Probe';
  }
});

// Auto-refresh every 15 seconds for live real-time feel
loadDashboard();
setInterval(loadDashboard, 15000);
