import { api } from './api.js';
import { auth } from './auth.js';
import { renderLayout, showToast, formatDuration } from './common.js';

auth.requireAuth();
renderLayout('Analytics');

let currentDays = 1;
let latencyChartInstance = null;
let uptimeChartInstance = null;

function formatTimeLabel(timestampStr) {
  if (!timestampStr) return '';
  try {
    const d = new Date(timestampStr);
    if (!isNaN(d.getTime())) {
      if (currentDays === 1) {
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

async function loadAnalytics() {
  await Promise.all([
    loadUptimeData(),
    loadLatencyData(),
    loadIncidentData()
  ]);
}

async function loadUptimeData() {
  try {
    const res = await api.get('/analytics/uptime', { days: currentDays });

    const totalChecks = res.totalChecks || 0;
    document.getElementById('an-uptime').innerText = totalChecks > 0 ? `${res.uptimePercent}%` : '0%';
    document.getElementById('an-checks').innerText = totalChecks > 0 ? `${res.successfulChecks} successful · ${res.failedChecks} failed` : '0 checks analyzed';
    document.getElementById('an-downtime').innerText = totalChecks > 0 ? formatDuration(res.downtimeDurationSeconds) : '0s';

    // Render Uptime Chart
    const canvas = document.getElementById('analyticsUptimeChart');
    if (!canvas) return;
    const parent = canvas.parentElement;

    if (uptimeChartInstance) {
      uptimeChartInstance.destroy();
      uptimeChartInstance = null;
    }

    if (totalChecks === 0 || !res.trend || res.trend.length === 0) {
      canvas.style.display = 'none';
      let emptyMsg = parent.querySelector('.empty-uptime-msg');
      if (!emptyMsg) {
        emptyMsg = document.createElement('div');
        emptyMsg.className = 'empty-uptime-msg';
        emptyMsg.style.cssText = 'display:flex; flex-direction:column; align-items:center; justify-content:center; height:100%; color:var(--text-muted); gap:8px;';
        parent.appendChild(emptyMsg);
      }
      emptyMsg.innerHTML = `
        <svg viewBox="0 0 24 24" width="36" height="36" stroke="currentColor" stroke-width="1.5" fill="none" style="opacity:0.4;"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
        <span style="font-weight:600; font-size:14px; color:var(--text-main, #334155);">No Availability Data Available</span>
        <span style="font-size:12px; color:var(--text-dim, #64748B);">Create your first monitor to begin recording availability history.</span>
      `;
      emptyMsg.style.display = 'flex';
      return;
    }

    const existingMsg = parent.querySelector('.empty-uptime-msg');
    if (existingMsg) existingMsg.style.display = 'none';
    canvas.style.display = 'block';

    const ctx = canvas.getContext('2d');
    const rawLabels = res.trend.map(t => t.timestamp);
    const formattedLabels = rawLabels.map(t => formatTimeLabel(t));
    const values = res.trend.map(t => t.uptime);

    if (window.Chart) {
      uptimeChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
          labels: formattedLabels,
          datasets: [{
            label: 'Availability %',
            data: values,
            backgroundColor: values.map(v => v >= 99 ? 'rgba(16, 185, 129, 0.85)' : (v >= 95 ? 'rgba(37, 99, 235, 0.85)' : (v >= 90 ? 'rgba(245, 158, 11, 0.85)' : 'rgba(239, 68, 68, 0.85)'))),
            hoverBackgroundColor: values.map(v => v >= 99 ? '#10B981' : (v >= 95 ? '#2563EB' : (v >= 90 ? '#D97706' : '#DC2626'))),
            borderRadius: { topLeft: 6, topRight: 6 },
            borderSkipped: false,
            barPercentage: 0.65,
            categoryPercentage: 0.8
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
              min: 0,
              max: 100,
              grid: { color: 'rgba(226, 232, 240, 0.7)' },
              ticks: {
                stepSize: 25,
                color: '#64748B',
                font: { size: 11, weight: '500' },
                callback: (val) => `${val}%`
              }
            }
          },
          plugins: {
            legend: { display: false },
            tooltip: {
              backgroundColor: '#0F172A',
              titleColor: '#F8FAFC',
              bodyColor: '#10B981',
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
                label: (ctx) => `Availability: ${ctx.parsed.y}%`
              }
            }
          }
        }
      });
    }

  } catch (e) {
    console.warn('Failed to load uptime data:', e);
  }
}

async function loadLatencyData() {
  try {
    const res = await api.get('/analytics/response-times', { days: currentDays });

    const hasData = res.trend && res.trend.length > 0;
    document.getElementById('an-avg').innerText = hasData ? `${res.avgMs} ms` : '0 ms';
    document.getElementById('an-min-max').innerText = hasData ? `Min: ${res.minMs}ms · Max: ${res.maxMs}ms` : 'Min: 0ms · Max: 0ms';
    document.getElementById('an-median').innerText = hasData ? `${res.medianMs} ms` : '0 ms';
    document.getElementById('an-p95').innerText = hasData ? `${res.p95Ms} ms` : '0 ms';

    // Render Latency Trend Chart
    const canvas = document.getElementById('analyticsLatencyChart');
    if (!canvas) return;
    const parent = canvas.parentElement;

    if (latencyChartInstance) {
      latencyChartInstance.destroy();
      latencyChartInstance = null;
    }

    if (!hasData) {
      canvas.style.display = 'none';
      let emptyMsg = parent.querySelector('.empty-latency-msg');
      if (!emptyMsg) {
        emptyMsg = document.createElement('div');
        emptyMsg.className = 'empty-latency-msg';
        emptyMsg.style.cssText = 'display:flex; flex-direction:column; align-items:center; justify-content:center; height:100%; color:var(--text-muted); gap:8px;';
        parent.appendChild(emptyMsg);
      }
      emptyMsg.innerHTML = `
        <svg viewBox="0 0 24 24" width="36" height="36" stroke="currentColor" stroke-width="1.5" fill="none" style="opacity:0.4;"><circle cx="12" cy="12" r="10"></circle><polyline points="12 6 12 12 16 14"></polyline></svg>
        <span style="font-weight:600; font-size:14px; color:var(--text-main, #334155);">No Response Time Metrics Recorded</span>
        <span style="font-size:12px; color:var(--text-dim, #64748B);">Live latency percentiles (p50, p95) will appear here once monitoring begins.</span>
      `;
      emptyMsg.style.display = 'flex';
      return;
    }

    const existingMsg = parent.querySelector('.empty-latency-msg');
    if (existingMsg) existingMsg.style.display = 'none';
    canvas.style.display = 'block';

    const ctx = canvas.getContext('2d');
    const rawLabels = res.trend.map(p => p.timestamp);
    const formattedLabels = rawLabels.map(t => formatTimeLabel(t));
    const values = res.trend.map(p => p.latencyMs);

    // Create Soft Blue Gradient Area
    const gradient = ctx.createLinearGradient(0, 0, 0, 260);
    gradient.addColorStop(0, 'rgba(37, 99, 235, 0.22)');
    gradient.addColorStop(1, 'rgba(37, 99, 235, 0.00)');

    if (window.Chart) {
      latencyChartInstance = new Chart(ctx, {
        type: 'line',
        data: {
          labels: formattedLabels,
          datasets: [{
            label: 'Latency (ms)',
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
    console.warn('Failed to load latency data:', e);
  }
}

async function loadIncidentData() {
  try {
    const res = await api.get('/analytics/incidents');
    const total = res.totalIncidents || 0;
    const resolved = res.resolvedIncidents || 0;
    document.getElementById('an-incidents').innerText = total;
    document.getElementById('an-inc-sub').innerText = total > 0 ? `${res.openIncidents} OPEN · ${resolved} Resolved` : 'Across active monitors';
    document.getElementById('an-mttr').innerText = resolved > 0 ? formatDuration(res.avgMttrSeconds) : '0s';
  } catch (e) {
    console.warn(e);
  }
}

// Time Range Selector
document.querySelectorAll('.range-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.range-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    currentDays = parseInt(btn.getAttribute('data-days'));
    loadAnalytics();
  });
});

// CSV Export
document.getElementById('export-csv-btn')?.addEventListener('click', () => {
  const token = localStorage.getItem('pulsewatch_access_token');
  fetch(`/api/v1/analytics/export?days=${currentDays}`, {
    headers: { 'Authorization': `Bearer ${token}` }
  })
  .then(res => res.blob())
  .then(blob => {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `pulsewatch_analytics_${currentDays}d_${Date.now()}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    showToast('CSV export downloaded', 'success');
  })
  .catch(err => showToast('Failed to export CSV', 'error'));
});

loadAnalytics();
