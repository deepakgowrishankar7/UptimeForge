import { api } from './api.js';
import { auth } from './auth.js';
import { renderLayout, showToast, getStatusBadge, formatDate, formatDuration, escapeHtml, openModal, closeModal } from './common.js';

auth.requireAuth();
renderLayout('Incidents');

let selectedIncidentId = null;

async function loadIncidents() {
  const tbody = document.getElementById('incidents-list');
  const status = document.getElementById('incident-status-filter').value;

  try {
    const params = { size: 50 };
    if (status) params.status = status;

    const data = await api.get('/incidents', params);
    const incidents = data.content || [];

    if (incidents.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; color:var(--text-dim); padding:36px;">No incidents found matching current filter.</td></tr>';
      return;
    }

    tbody.innerHTML = incidents.map(inc => `
      <tr>
        <td>${getStatusBadge(inc.status)}</td>
        <td><span style="font-weight:700; color:${getSeverityColor(inc.severity)};">${inc.severity}</span></td>
        <td>
          <a href="/monitor-details.html?id=${inc.monitorId}" style="font-weight:600;">${escapeHtml(inc.monitorName)}</a>
        </td>
        <td style="font-size:12px;">${formatDate(inc.startedAt)}</td>
        <td style="font-size:12px; color:var(--text-muted);">${formatDate(inc.detectedAt)}</td>
        <td>${formatDuration(inc.durationSeconds)}</td>
        <td style="font-size:12px;">${escapeHtml(inc.acknowledgedByName || 'Unassigned')}</td>
        <td style="text-align: right;">
          <button class="btn btn-secondary btn-sm view-btn" data-id="${inc.id}">Timeline</button>
          ${inc.status === 'OPEN' ? `
            <button class="btn btn-secondary btn-sm ack-btn" data-id="${inc.id}">Ack</button>
          ` : ''}
          ${inc.status !== 'RESOLVED' ? `
            <button class="btn btn-primary btn-sm resolve-btn" data-id="${inc.id}">Resolve</button>
          ` : ''}
        </td>
      </tr>
    `).join('');

    attachTableEvents();

  } catch (err) {
    showToast(err.message || 'Failed to load incidents', 'error');
  }
}

function getSeverityColor(sev) {
  if (sev === 'CRITICAL') return 'var(--color-down)';
  if (sev === 'HIGH') return '#f97316';
  if (sev === 'MEDIUM') return 'var(--color-degraded)';
  return 'var(--accent-cyan)';
}

function attachTableEvents() {
  document.querySelectorAll('.view-btn').forEach(btn => {
    btn.addEventListener('click', () => openIncidentModal(btn.getAttribute('data-id')));
  });

  document.querySelectorAll('.ack-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      try {
        await api.post(`/incidents/${id}/acknowledge`);
        showToast('Incident acknowledged', 'success');
        loadIncidents();
      } catch (e) {
        showToast(e.message || 'Failed to acknowledge', 'error');
      }
    });
  });

  document.querySelectorAll('.resolve-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      selectedIncidentId = btn.getAttribute('data-id');
      openModal('resolve-modal');
    });
  });
}

async function openIncidentModal(id) {
  selectedIncidentId = id;
  try {
    const inc = await api.get(`/incidents/${id}`);
    const events = await api.get(`/incidents/${id}/events`);
    const notes = await api.get(`/incidents/${id}/notes`);

    document.getElementById('inc-modal-title').innerText = `Incident #${inc.id} — ${inc.monitorName}`;
    document.getElementById('inc-summary').innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <div>
          <strong>Status:</strong> ${getStatusBadge(inc.status)} &nbsp;
          <strong>Severity:</strong> ${inc.severity} &nbsp;
          <strong>Duration:</strong> ${formatDuration(inc.durationSeconds)}
        </div>
        <div style="font-size:12px; color:var(--text-dim);">Started: ${formatDate(inc.startedAt)}</div>
      </div>
    `;

    // Render Events Timeline
    const timelineEl = document.getElementById('inc-timeline');
    if (!events || events.length === 0) {
      timelineEl.innerHTML = '<div style="color:var(--text-dim); font-size:13px;">No timeline events recorded yet.</div>';
    } else {
      timelineEl.innerHTML = events.map(e => `
        <div style="padding:10px 0; border-left:2px solid var(--border-color); padding-left:14px; margin-left:8px; position:relative;">
          <div style="position:absolute; left:-6px; top:14px; width:10px; height:10px; border-radius:50%; background:var(--accent-cyan);"></div>
          <div style="display:flex; justify-content:space-between; font-size:12px; color:var(--text-dim); margin-bottom:4px;">
            <strong style="color:var(--text-main);">${e.eventType}</strong>
            <span>${formatDate(e.createdAt)}</span>
          </div>
          <div style="font-size:13px; color:var(--text-muted);">${escapeHtml(e.description)}</div>
        </div>
      `).join('');
    }

    // Render Notes
    const notesEl = document.getElementById('inc-notes');
    if (!notes || notes.length === 0) {
      notesEl.innerHTML = '<div style="color:var(--text-dim); font-size:13px;">No internal notes yet.</div>';
    } else {
      notesEl.innerHTML = notes.map(n => `
        <div style="background:var(--bg-card-hover); padding:10px 14px; border-radius:var(--radius-sm); margin-bottom:8px;">
          <div style="display:flex; justify-content:space-between; font-size:12px; margin-bottom:4px;">
            <strong style="color:var(--accent-cyan);">${escapeHtml(n.authorName)}</strong>
            <span style="color:var(--text-dim);">${formatDate(n.createdAt)}</span>
          </div>
          <div style="font-size:13px;">${escapeHtml(n.note)}</div>
        </div>
      `).join('');
    }

    // Actions Footer
    const actionsFooter = document.getElementById('inc-modal-actions');
    actionsFooter.innerHTML = `
      ${inc.status === 'OPEN' ? `<button class="btn btn-secondary modal-ack-btn">Acknowledge</button>` : ''}
      ${inc.status !== 'RESOLVED' ? `<button class="btn btn-primary modal-res-btn">Resolve Incident</button>` : ''}
      <button class="btn btn-secondary" id="modal-close-action">Close</button>
    `;

    document.getElementById('modal-close-action')?.addEventListener('click', () => closeModal('incident-modal'));

    actionsFooter.querySelector('.modal-ack-btn')?.addEventListener('click', async () => {
      try {
        await api.post(`/incidents/${selectedIncidentId}/acknowledge`);
        showToast('Acknowledged', 'success');
        openIncidentModal(selectedIncidentId);
        loadIncidents();
      } catch (e) {
        showToast(e.message || 'Error', 'error');
      }
    });

    actionsFooter.querySelector('.modal-res-btn')?.addEventListener('click', () => {
      closeModal('incident-modal');
      openModal('resolve-modal');
    });

    openModal('incident-modal');

  } catch (err) {
    showToast(err.message || 'Failed to load details', 'error');
  }
}

// Add Note Form
document.getElementById('add-note-form')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const input = document.getElementById('note-input');
  const note = input.value.trim();
  if (!note || !selectedIncidentId) return;

  try {
    await api.post(`/incidents/${selectedIncidentId}/notes`, { note });
    input.value = '';
    showToast('Note added', 'success');
    openIncidentModal(selectedIncidentId);
  } catch (err) {
    showToast(err.message || 'Failed to add note', 'error');
  }
});

// Resolve Form
document.getElementById('resolve-form')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const notes = document.getElementById('resolution-notes').value.trim();
  if (!selectedIncidentId) return;

  try {
    await api.post(`/incidents/${selectedIncidentId}/resolve`, { resolutionNotes: notes });
    showToast('Incident resolved successfully', 'success');
    closeModal('resolve-modal');
    document.getElementById('resolve-form').reset();
    loadIncidents();
  } catch (err) {
    showToast(err.message || 'Failed to resolve', 'error');
  }
});

document.getElementById('close-inc-modal')?.addEventListener('click', () => closeModal('incident-modal'));
document.getElementById('close-resolve-modal')?.addEventListener('click', () => closeModal('resolve-modal'));
document.getElementById('cancel-resolve-btn')?.addEventListener('click', () => closeModal('resolve-modal'));

document.getElementById('incident-status-filter')?.addEventListener('change', () => loadIncidents());
document.getElementById('refresh-incidents-btn')?.addEventListener('click', () => loadIncidents());

loadIncidents();
