import { api } from './api.js';
import { auth } from './auth.js';
import { renderLayout, showToast, showConfirmModal, getStatusBadge, formatDate, escapeHtml, openModal, closeModal } from './common.js';

auth.requireAuth();
renderLayout('Monitors');

let currentStatusFilter = '';
let currentSearchQuery = '';

async function loadMonitors() {
  const tbody = document.getElementById('monitors-list');
  try {
    const params = { size: 50 };
    if (currentStatusFilter) params.status = currentStatusFilter;
    if (currentSearchQuery) params.q = currentSearchQuery;

    const data = await api.get('/monitors', params);
    const monitors = data.content || [];

    if (monitors.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; color:var(--text-dim); padding: 36px;">No monitors match the current filter.</td></tr>';
      return;
    }

    tbody.innerHTML = monitors.map(m => `
      <tr>
        <td>${getStatusBadge(m.currentStatus)}</td>
        <td>
          <a href="/monitor-details.html?id=${m.id}" style="font-weight:600; font-size:14px; display:block;">${escapeHtml(m.name)}</a>
          <span style="font-size:12px; color:var(--text-dim);">${escapeHtml(m.url)}</span>
        </td>
        <td><span style="font-size:12px; font-weight:600; color:var(--accent-cyan);">${m.type}</span></td>
        <td>${m.intervalSeconds}s</td>
        <td><strong>${m.lastResponseTimeMs !== null ? m.lastResponseTimeMs + ' ms' : '—'}</strong></td>
        <td><span style="color:${m.uptime24h > 99 ? 'var(--color-up)' : 'var(--color-degraded)'}; font-weight:600;">${m.uptime24h !== null ? m.uptime24h + '%' : 'N/A'}</span></td>
        <td style="font-size:12px; color:var(--text-muted);">${formatDate(m.lastCheckAt)}</td>
        <td style="text-align: right; display:flex; gap:6px; justify-content:flex-end;">
          <button class="btn btn-secondary btn-sm test-btn" data-id="${m.id}" title="Run Check Now" style="padding:4px 8px;">
            <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon></svg>
          </button>
          ${m.enabled ? `
            <button class="btn btn-secondary btn-sm pause-btn" data-id="${m.id}" title="Pause" style="padding:4px 8px;">
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><rect x="6" y="4" width="4" height="16"></rect><rect x="14" y="4" width="4" height="16"></rect></svg>
            </button>
          ` : `
            <button class="btn btn-secondary btn-sm resume-btn" data-id="${m.id}" title="Resume" style="padding:4px 8px;">
              <svg viewBox="0 0 24 24" width="14" height="14" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg>
            </button>
          `}
          <a href="/monitor-details.html?id=${m.id}" class="btn btn-secondary btn-sm" title="Inspect Monitor">Details</a>
          <button class="btn btn-danger btn-sm del-btn" data-id="${m.id}" title="Delete">&times;</button>
        </td>
      </tr>
    `).join('');

    attachRowEvents();

  } catch (err) {
    showToast(err.message || 'Failed to load monitors', 'error');
  }
}

function attachRowEvents() {
  document.querySelectorAll('.test-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      btn.disabled = true;
      showToast('Executing health probe...', 'info');
      try {
        const res = await api.post(`/monitors/${id}/test-check`);
        if (res.success) {
          showToast(`Probe Succeeded! Code: ${res.statusCode || 200}, Time: ${res.responseTimeMs}ms`, 'success');
        } else {
          showToast(`Probe Failed: ${res.errorType} - ${res.errorMessage}`, 'error');
        }
        loadMonitors();
      } catch (e) {
        showToast(e.message || 'Test failed', 'error');
      } finally {
        btn.disabled = false;
      }
    });
  });

  document.querySelectorAll('.pause-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      try {
        await api.post(`/monitors/${id}/pause`);
        showToast('Monitor paused', 'info');
        loadMonitors();
      } catch (e) {
        showToast(e.message || 'Failed to pause', 'error');
      }
    });
  });

  document.querySelectorAll('.resume-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      try {
        await api.post(`/monitors/${id}/resume`);
        showToast('Monitor resumed', 'success');
        loadMonitors();
      } catch (e) {
        showToast(e.message || 'Failed to resume', 'error');
      }
    });
  });

  document.querySelectorAll('.del-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      const confirmed = await showConfirmModal({
        title: 'Delete Monitor?',
        message: 'Are you sure you want to delete this monitor? All historical logs and incident history will be permanently deleted.',
        confirmText: 'Delete Monitor',
        confirmColor: 'danger'
      });
      if (confirmed) {
        try {
          await api.del(`/monitors/${id}`);
          showToast('Monitor deleted', 'success');
          loadMonitors();
        } catch (e) {
          showToast(e.message || 'Failed to delete monitor', 'error');
        }
      }
    });
  });
}

// Search and filter inputs
document.getElementById('search-input')?.addEventListener('input', (e) => {
  currentSearchQuery = e.target.value.trim();
  loadMonitors();
});
document.getElementById('status-filter')?.addEventListener('change', (e) => {
  currentStatusFilter = e.target.value;
  loadMonitors();
});
document.getElementById('refresh-btn')?.addEventListener('click', () => loadMonitors());

// Modal controls
document.getElementById('add-monitor-btn')?.addEventListener('click', () => openModal('create-modal'));
document.getElementById('close-modal')?.addEventListener('click', () => closeModal('create-modal'));
document.getElementById('cancel-btn')?.addEventListener('click', () => closeModal('create-modal'));

document.getElementById('create-form')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const saveBtn = document.getElementById('save-btn');
  saveBtn.disabled = true;

  const thresholdElem = document.getElementById('failureThreshold');
  const payload = {
    name: document.getElementById('name').value.trim(),
    type: document.getElementById('type').value,
    httpMethod: document.getElementById('httpMethod').value,
    url: document.getElementById('url').value.trim(),
    intervalSeconds: parseInt(document.getElementById('intervalSeconds').value),
    timeoutMs: parseInt(document.getElementById('timeoutMs').value),
    failureThreshold: thresholdElem ? parseInt(thresholdElem.value) : 1,
    recoveryThreshold: 1,
    expectedStatusCodes: document.getElementById('expectedStatusCodes').value.trim(),
    expectedText: document.getElementById('expectedText').value.trim() || null
  };

  try {
    await api.post('/monitors', payload);
    showToast('Monitor created successfully', 'success');
    closeModal('create-modal');
    document.getElementById('create-form').reset();
    loadMonitors();
  } catch (err) {
    showToast(err.message || 'Failed to create monitor', 'error');
  } finally {
    saveBtn.disabled = false;
  }
});

loadMonitors();
setInterval(loadMonitors, 20000);
