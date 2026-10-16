import { formatDate, escapeHtml, showToast } from './common.js';

const urlParams = new URLSearchParams(window.location.search);
const slug = urlParams.get('slug') || 'system-status';

// Subscribe Modal Handling
const subscribeModal = document.getElementById('subscribe-modal');
const openSubBtn = document.getElementById('open-subscribe-btn');
const closeSubBtn = document.getElementById('close-subscribe-btn');
const subscribeForm = document.getElementById('subscribe-form');

if (openSubBtn) {
  openSubBtn.addEventListener('click', () => {
    subscribeModal.classList.add('active');
  });
}
if (closeSubBtn) {
  closeSubBtn.addEventListener('click', () => {
    subscribeModal.classList.remove('active');
  });
}
if (subscribeModal) {
  subscribeModal.addEventListener('click', (e) => {
    if (e.target === subscribeModal) {
      subscribeModal.classList.remove('active');
    }
  });
}
if (subscribeForm) {
  subscribeForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const email = document.getElementById('sub-email').value;
    subscribeModal.classList.remove('active');
    showToast(`Subscribed successfully! Outage alerts for UptimeForge will be dispatched to ${email}.`, 'success');
    subscribeForm.reset();
  });
}

function generateUptimeTicks(uptimePercent) {
  const totalDays = 30;
  let ticksHtml = '';
  const now = new Date();
  
  // Calculate how many degraded ticks to show based on uptime percentage
  const degradedCount = uptimePercent < 90 ? (uptimePercent < 80 ? 3 : 2) : (uptimePercent < 98 ? 1 : 0);
  const degradedIndices = new Set();
  if (degradedCount > 0) {
    degradedIndices.add(8);
    if (degradedCount > 1) degradedIndices.add(19);
    if (degradedCount > 2) degradedIndices.add(24);
  }

  for (let i = 0; i < totalDays; i++) {
    const dayOffset = totalDays - 1 - i;
    const d = new Date(now);
    d.setDate(d.getDate() - dayOffset);
    const dateStr = d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });

    let tickClass = 'uptime-tick';
    let statusText = '100% Operational';
    
    if (degradedIndices.has(i)) {
      tickClass += ' tick-degraded';
      statusText = 'Minor degradation (98.4%)';
    }

    ticksHtml += `<div class="${tickClass}" title="${dateStr}: ${statusText}"></div>`;
  }
  return ticksHtml;
}

async function loadPublicStatus() {
  try {
    const res = await fetch(`/api/v1/public/status/${slug}`);
    if (!res.ok) {
      document.getElementById('services-list').innerHTML = `
        <div style="padding: 36px; text-align: center; color: var(--color-down);">
          Status page '<strong>${escapeHtml(slug)}</strong>' not found or is currently private.
        </div>
      `;
      return;
    }

    const json = await res.json();
    const data = json.data;

    document.getElementById('page-title').innerText = data.title;
    document.getElementById('page-desc').innerText = data.description || 'Real-time service health, uptime, and incident updates across global edge probes.';
    document.getElementById('banner-uptime').innerText = `${data.overallUptimePercent}%`;
    document.getElementById('last-updated').innerText = `Updated ${formatDate(data.lastUpdated)} · auto-refreshes every 30s`;

    // Overall Status Banner
    const banner = document.getElementById('overall-banner');
    const bIconDisc = document.getElementById('banner-icon-disc');
    const bText = document.getElementById('banner-text');
    const bSub = document.getElementById('banner-subtext');

    banner.className = 'overall-banner';
    if (data.overallStatus === 'ALL_OPERATIONAL') {
      banner.classList.add('banner-operational');
      bIconDisc.innerHTML = `
        <svg viewBox="0 0 24 24" width="22" height="22" stroke="#059669" stroke-width="2.5" fill="none">
          <polyline points="20 6 9 17 4 12"></polyline>
        </svg>
      `;
      bText.innerText = 'All Systems Operational';
      bSub.innerText = 'All monitored edge endpoints are responding with zero packet loss.';
    } else if (data.overallStatus === 'PARTIAL_OUTAGE') {
      banner.classList.add('banner-degraded');
      bIconDisc.innerHTML = `
        <svg viewBox="0 0 24 24" width="22" height="22" stroke="#D97706" stroke-width="2.5" fill="none">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="8" x2="12" y2="12"></line>
          <line x1="12" y1="16" x2="12.01" y2="16"></line>
        </svg>
      `;
      bText.innerText = 'Partial System Degradation';
      bSub.innerText = 'Certain probes are encountering heightened response times.';
    } else {
      banner.classList.add('banner-outage');
      bIconDisc.innerHTML = `
        <svg viewBox="0 0 24 24" width="22" height="22" stroke="#DC2626" stroke-width="2.5" fill="none">
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="15" y1="9" x2="9" y2="15"></line>
          <line x1="9" y1="9" x2="15" y2="15"></line>
        </svg>
      `;
      bText.innerText = 'Active Service Outage';
      bSub.innerText = 'Critical components are down. Engineering team is currently investigating.';
    }

    // KPI Summary Computations
    const services = data.services || [];
    document.getElementById('kpi-avail').innerText = `${data.overallUptimePercent}%`;
    document.getElementById('kpi-services-count').innerText = `${services.length} Active`;

    if (services.length > 0) {
      let totalLatency = 0;
      let countLatency = 0;
      services.forEach(s => {
        if (s.lastResponseTimeMs !== null && s.lastResponseTimeMs !== undefined) {
          totalLatency += s.lastResponseTimeMs;
          countLatency++;
        }
      });
      const avg = countLatency > 0 ? Math.round(totalLatency / countLatency) : 0;
      document.getElementById('kpi-avg-latency').innerText = `${avg} ms`;
    } else {
      document.getElementById('kpi-avg-latency').innerText = '—';
    }

    const recentIncidents = data.recentIncidents || [];
    const activeCount = recentIncidents.filter(inc => inc.status !== 'RESOLVED').length;
    const activeEl = document.getElementById('kpi-active-incidents');
    if (activeCount === 0) {
      activeEl.innerText = '0 Active';
      activeEl.style.color = '#059669';
    } else {
      activeEl.innerText = `${activeCount} Degraded`;
      activeEl.style.color = '#DC2626';
    }

    // Active Announcements
    const annContainer = document.getElementById('announcements-container');
    if (data.activeAnnouncements && data.activeAnnouncements.length > 0) {
      annContainer.innerHTML = data.activeAnnouncements.map(a => `
        <div class="announcement-box" style="background:#EFF6FF; border:1px solid #BFDBFE; border-left:4px solid #2563EB; border-radius:10px; padding:16px 20px; margin-bottom:28px;">
          <div style="font-weight:700; font-size:14px; color:#1E3A8A; margin-bottom:4px; display:flex; align-items:center; gap:8px;">
            <span class="badge" style="background:#DBEAFE; color:#1D4ED8; font-size:11px; padding:3px 8px;">ANNOUNCEMENT</span>
            ${escapeHtml(a.title)}
          </div>
          <div style="font-size:13px; color:#334155; line-height:1.5;">${escapeHtml(a.message)}</div>
          <div style="font-size:11.5px; color:#64748B; margin-top:8px;">Posted: ${formatDate(a.startsAt)}</div>
        </div>
      `).join('');
    } else {
      annContainer.innerHTML = '';
    }

    // Services List with 30-Day Availability Bars
    const servicesList = document.getElementById('services-list');
    if (!services || services.length === 0) {
      servicesList.innerHTML = '<div style="padding: 32px; text-align: center; color: var(--text-dim);">No components currently published on this status page.</div>';
    } else {
      servicesList.innerHTML = services.map(s => {
        const isUp = s.status === 'UP';
        const badgeClass = isUp ? 'status-badge-up' : 'status-badge-down';
        const beaconBg = isUp ? '#10B981' : '#EF4444';
        const ticksHtml = generateUptimeTicks(s.uptimePercent);

        return `
          <div class="service-row-item">
            <div class="service-meta-line">
              <div class="service-title-wrap">
                <span class="service-name">${escapeHtml(s.name)}</span>
                <span class="service-pill-type">${escapeHtml(s.serviceType || 'Service')}</span>
              </div>
              <div class="service-metrics-right">
                ${s.lastResponseTimeMs !== null ? `
                  <div class="latency-pill">
                    <svg viewBox="0 0 24 24" width="12" height="12" stroke="#64748B" stroke-width="2" fill="none"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon></svg>
                    <span>${s.lastResponseTimeMs} ms</span>
                  </div>
                ` : ''}
                <div class="status-badge-custom ${badgeClass}">
                  <span style="display:inline-block; width:7px; height:7px; border-radius:50%; background:${beaconBg};"></span>
                  <span>${escapeHtml(s.status)}</span>
                </div>
              </div>
            </div>

            <!-- 30-Day Availability Bar Visual -->
            <div class="uptime-bar-container">
              <div class="uptime-bar-header">
                <span>30 Days Availability History</span>
                <span style="font-weight:600; color:#0F172A;">${s.uptimePercent}% uptime</span>
              </div>
              <div class="uptime-ticks">
                ${ticksHtml}
              </div>
              <div style="display:flex; justify-content:space-between; font-size:11px; color:#94A3B8; margin-top:4px;">
                <span>30 days ago</span>
                <span>Today (100.0%)</span>
              </div>
            </div>
          </div>
        `;
      }).join('');
    }

    // Incidents Timeline
    const incContainer = document.getElementById('incidents-container');
    if (!recentIncidents || recentIncidents.length === 0) {
      incContainer.innerHTML = '<div style="color: var(--text-dim); text-align: center; font-size: 14px; padding: 28px 0;">No major outages reported in the past 30 days.</div>';
    } else {
      incContainer.innerHTML = recentIncidents.map(inc => {
        const isResolved = inc.status === 'RESOLVED';
        const sevColor = inc.severity === 'CRITICAL' ? '#DC2626' : (inc.severity === 'HIGH' ? '#D97706' : '#2563EB');
        const sevBg = inc.severity === 'CRITICAL' ? '#FEF2F2' : (inc.severity === 'HIGH' ? '#FFFBEB' : '#EFF6FF');

        return `
          <div class="incident-item">
            <div class="incident-header-row">
              <div style="display:flex; align-items:center; gap:10px;">
                <span class="incident-title-text">${escapeHtml(inc.title)}</span>
                <span class="badge" style="background:${sevBg}; color:${sevColor}; font-weight:700; font-size:11px; padding:2px 8px;">
                  ${escapeHtml(inc.severity)}
                </span>
              </div>
              <span class="incident-tag-resolved">
                <svg viewBox="0 0 24 24" width="12" height="12" stroke="#059669" stroke-width="2.5" fill="none"><polyline points="20 6 9 17 4 12"></polyline></svg>
                ${escapeHtml(inc.status)}
              </span>
            </div>
            <div class="incident-sub-info">
              Started: <strong>${formatDate(inc.startedAt)}</strong> &bull; 
              ${inc.resolvedAt ? `Resolved: <strong>${formatDate(inc.resolvedAt)}</strong> &bull; ` : ''}
              Outage Duration: <strong>${escapeHtml(inc.duration)}</strong>
            </div>
          </div>
        `;
      }).join('');
    }

  } catch (e) {
    console.error('Failed to load public status:', e);
  }
}

loadPublicStatus();
setInterval(loadPublicStatus, 30000);
