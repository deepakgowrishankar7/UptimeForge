import { api } from './api.js';
import { auth } from './auth.js';
import { renderLayout, showToast, getStatusBadge, formatDate, formatDuration, escapeHtml, openModal, closeModal } from './common.js';

auth.requireAuth();
renderLayout('Monitors');

const urlParams = new URLSearchParams(window.location.search);
const monitorId = urlParams.get('id');

if (!monitorId) {
  window.location.href = '/monitors.html';
}

let currentDays = 1;
let chartInstance = null;
let currentMonitor = null;

async function loadAllDetails() {
  await Promise.all([
    loadMonitorConfig(),
    loadUptime(),
    loadChart(currentDays),
    loadResults(),
    loadIncidents()
  ]);
}

async function loadMonitorConfig() {
  try {
    currentMonitor = await api.get(`/monitors/${monitorId}`);
    
    document.getElementById('page-title').innerText = currentMonitor.name;
    document.getElementById('monitor-status-badge').innerHTML = getStatusBadge(currentMonitor.currentStatus);

    document.getElementById('det-latency').innerText = currentMonitor.lastResponseTimeMs !== null ? `${currentMonitor.lastResponseTimeMs} ms` : '—';
    document.getElementById('det-last-check').innerText = `Last check: ${formatDate(currentMonitor.lastCheckAt)}`;
    document.getElementById('det-interval').innerText = `${currentMonitor.intervalSeconds}s`;
    document.getElementById('det-timeout').innerText = `Timeout: ${currentMonitor.timeoutMs / 1000}s`;
    
    const consecutiveText = currentMonitor.currentStatus === 'UP' 
      ? `${currentMonitor.consecutiveSuccesses} consecutive successes`
      : `${currentMonitor.consecutiveFailures} consecutive failures`;
    document.getElementById('det-consecutive').innerText = currentMonitor.currentStatus === 'UP' 
      ? currentMonitor.consecutiveSuccesses 
      : currentMonitor.consecutiveFailures;
    document.getElementById('det-thresholds').innerText = consecutiveText;

    // Configuration panel
    document.getElementById('conf-url').innerText = currentMonitor.url;
    document.getElementById('conf-method').innerText = `${currentMonitor.type} (${currentMonitor.httpMethod})`;
    document.getElementById('conf-status').innerText = currentMonitor.expectedStatusCodes;
    document.getElementById('conf-text').innerText = currentMonitor.expectedText || 'None';
    document.getElementById('conf-next').innerText = formatDate(currentMonitor.nextCheckAt);

    // Pause button state
    const pauseBtn = document.getElementById('pause-toggle-btn');
    if (currentMonitor.enabled) {
      pauseBtn.innerText = 'Pause Monitor';
    } else {
      pauseBtn.innerText = 'Resume Monitor';
    }

  } catch (err) {
    showToast(err.message || 'Failed to load monitor details', 'error');
  }
}

async function loadUptime() {
  try {
    const res = await api.get(`/monitors/${monitorId}/uptime`, { days: currentDays });
    document.getElementById('det-uptime').innerText = `${res.uptimePercent}%`;
    document.getElementById('det-checks-count').innerText = `${res.successfulChecks} passed / ${res.totalChecks} total`;
  } catch (e) {
    console.warn(e);
  }
}

function formatDetailsTime(timestampStr, days) {
  if (!timestampStr) return '';
  try {
    const d = new Date(timestampStr);
    if (!isNaN(d.getTime())) {
      if (days === 1) {
        return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
      }
      return `${d.getMonth() + 1}/${d.getDate()} ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })}`;
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

async function loadChart(days) {
  try {
    const res = await api.get('/analytics/response-times', { monitorId, days });
    const canvas = document.getElementById('detailsChart');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');

    const rawLabels = res.trend ? res.trend.map(p => p.timestamp) : [];
    const formattedLabels = rawLabels.map(t => formatDetailsTime(t, days));
    const values = res.trend ? res.trend.map(p => p.latencyMs) : [];

    if (chartInstance) chartInstance.destroy();

    const gradient = ctx.createLinearGradient(0, 0, 0, 260);
    gradient.addColorStop(0, 'rgba(37, 99, 235, 0.22)');
    gradient.addColorStop(1, 'rgba(37, 99, 235, 0.00)');

    if (window.Chart) {
      chartInstance = new Chart(ctx, {
        type: 'line',
        data: {
          labels: formattedLabels.length > 0 ? formattedLabels : ['No Data'],
          datasets: [{
            label: 'Latency (ms)',
            data: values.length > 0 ? values : [0],
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
    console.warn('Failed to render chart:', e);
  }
}

async function loadResults() {
  const tbody = document.getElementById('results-tbody');
  try {
    const data = await api.get(`/monitors/${monitorId}/results`, { size: 30 });
    const results = data.content || [];

    if (results.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:var(--text-dim); padding:24px;">No health checks executed yet.</td></tr>';
      return;
    }

    tbody.innerHTML = results.map(r => `
      <tr>
        <td>
          ${r.success 
            ? '<span class="badge badge-up"><span class="badge-dot"></span>SUCCESS</span>'
            : '<span class="badge badge-down"><span class="badge-dot"></span>FAILED</span>'}
        </td>
        <td style="font-size:12px; color:var(--text-muted);">${formatDate(r.checkedAt)}</td>
        <td><strong>${r.responseTimeMs} ms</strong></td>
        <td>${r.statusCode ? `<span style="font-family:monospace; font-weight:600;">${r.statusCode}</span>` : '—'}</td>
        <td style="color:${r.success ? 'var(--text-muted)' : 'var(--color-down)'}; font-weight:600;">${r.errorType || 'None'}</td>
        <td style="font-size:12px; color:var(--text-dim); max-width:260px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;" title="${escapeHtml(r.errorMessage || '')}">
          ${escapeHtml(r.errorMessage || '—')}
        </td>
      </tr>
    `).join('');
  } catch (e) {
    console.warn('Failed to load check results:', e);
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:var(--text-muted); padding:24px;">No health checks executed yet.</td></tr>';
  }
}

async function loadIncidents() {
  const tbody = document.getElementById('incidents-tbody');
  try {
    const data = await api.get(`/monitors/${monitorId}/incidents`, { size: 10 });
    const incidents = data.content || [];

    if (incidents.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color:var(--text-muted); padding:24px;">✓ No incident history recorded for this monitor — All checks operational.</td></tr>';
      return;
    }

    tbody.innerHTML = incidents.map(inc => `
      <tr>
        <td>${getStatusBadge(inc.status)}</td>
        <td style="font-size:12px;">${formatDate(inc.startedAt)}</td>
        <td style="font-size:12px;">${inc.resolvedAt ? formatDate(inc.resolvedAt) : 'Ongoing'}</td>
        <td>${formatDuration(inc.durationSeconds)}</td>
        <td style="font-size:12px; color:var(--text-muted);">${escapeHtml(inc.resolutionNotes || '—')}</td>
      </tr>
    `).join('');
  } catch (e) {
    console.warn('Failed to load incidents:', e);
    tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color:var(--text-muted); padding:24px;">✓ No incident history recorded for this monitor — All checks operational.</td></tr>';
  }
}

// Time Range Buttons
document.querySelectorAll('.range-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.range-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    currentDays = parseInt(btn.getAttribute('data-days'));
    loadUptime();
    loadChart(currentDays);
  });
});

// Run Check Now
document.getElementById('test-check-btn')?.addEventListener('click', async () => {
  const btn = document.getElementById('test-check-btn');
  btn.disabled = true;
  btn.innerText = 'Checking...';
  showToast('Executing health probe...', 'info');

  try {
    const res = await api.post(`/monitors/${monitorId}/test-check`);
    if (res.success) {
      showToast(`Probe Succeeded! Time: ${res.responseTimeMs}ms`, 'success');
    } else {
      showToast(`Probe Failed: ${res.errorType}`, 'error');
    }
    loadAllDetails();
  } catch (err) {
    showToast(err.message || 'Check failed', 'error');
  } finally {
    btn.disabled = false;
    btn.innerText = 'Run Check Now';
  }
});

// Pause / Resume Toggle
document.getElementById('pause-toggle-btn')?.addEventListener('click', async () => {
  if (!currentMonitor) return;
  try {
    if (currentMonitor.enabled) {
      await api.post(`/monitors/${monitorId}/pause`);
      showToast('Monitor paused', 'info');
    } else {
      await api.post(`/monitors/${monitorId}/resume`);
      showToast('Monitor resumed', 'success');
    }
    loadMonitorConfig();
  } catch (e) {
    showToast(e.message || 'Action failed', 'error');
  }
});

// Edit Modal
document.getElementById('edit-btn')?.addEventListener('click', () => {
  if (!currentMonitor) return;
  document.getElementById('edit-name').value = currentMonitor.name;
  document.getElementById('edit-url').value = currentMonitor.url;
  document.getElementById('edit-interval').value = currentMonitor.intervalSeconds;
  document.getElementById('edit-timeout').value = currentMonitor.timeoutMs;
  document.getElementById('edit-status-codes').value = currentMonitor.expectedStatusCodes;
  document.getElementById('edit-expected-text').value = currentMonitor.expectedText || '';
  openModal('edit-modal');
});

document.getElementById('close-edit-modal')?.addEventListener('click', () => closeModal('edit-modal'));
document.getElementById('cancel-edit-btn')?.addEventListener('click', () => closeModal('edit-modal'));

document.getElementById('edit-form')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const saveBtn = document.getElementById('save-edit-btn');
  saveBtn.disabled = true;

  const payload = {
    name: document.getElementById('edit-name').value.trim(),
    url: document.getElementById('edit-url').value.trim(),
    type: currentMonitor.type,
    httpMethod: currentMonitor.httpMethod,
    intervalSeconds: parseInt(document.getElementById('edit-interval').value),
    timeoutMs: parseInt(document.getElementById('edit-timeout').value),
    expectedStatusCodes: document.getElementById('edit-status-codes').value.trim(),
    expectedText: document.getElementById('edit-expected-text').value.trim() || null
  };

  try {
    await api.put(`/monitors/${monitorId}`, payload);
    showToast('Monitor updated successfully', 'success');
    closeModal('edit-modal');
    loadAllDetails();
  } catch (err) {
    showToast(err.message || 'Failed to update monitor', 'error');
  } finally {
    saveBtn.disabled = false;
  }
});

loadAllDetails();
setInterval(loadAllDetails, 20000);
