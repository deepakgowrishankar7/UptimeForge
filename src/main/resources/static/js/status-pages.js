import { api } from './api.js';
import { auth } from './auth.js';
import { renderLayout, showToast, showConfirmModal, escapeHtml, openModal, closeModal } from './common.js';

auth.requireAuth();
renderLayout('Status Pages');

let selectedSpId = null;

async function loadPages() {
  const tbody = document.getElementById('pages-tbody');
  try {
    const pages = await api.get('/status-pages');
    if (!pages || pages.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color:var(--text-dim); padding:24px;">No status pages created. Click "+ Create Status Page" to publish one.</td></tr>';
      return;
    }

    tbody.innerHTML = pages.map(p => `
      <tr>
        <td>
          <strong style="font-size:15px; color:var(--text-main);">${escapeHtml(p.title)}</strong>
          <span style="display:block; font-size:12px; color:var(--text-dim); font-family:monospace;">/${escapeHtml(p.slug)}</span>
        </td>
        <td>
          ${p.published 
            ? '<span class="badge badge-up"><span class="badge-dot"></span>PUBLISHED</span>' 
            : '<span class="badge badge-paused">UNPUBLISHED</span>'}
        </td>
        <td>${p.monitors ? p.monitors.length : 0} services</td>
        <td>
          <a href="/public-status.html?slug=${p.slug}" target="_blank" style="font-size:13px; font-weight:600;">
            View Public Page &nearr;
          </a>
        </td>
        <td style="text-align: right;">
          <button class="btn btn-secondary btn-sm ann-btn" data-id="${p.id}" title="Post Announcement">Post Update</button>
          ${p.published ? `
            <button class="btn btn-secondary btn-sm unpub-btn" data-id="${p.id}" title="Unpublish">Hide</button>
          ` : `
            <button class="btn btn-secondary btn-sm pub-btn" data-id="${p.id}" title="Publish">Publish</button>
          `}
          <button class="btn btn-danger btn-sm del-sp-btn" data-id="${p.id}">&times;</button>
        </td>
      </tr>
    `).join('');

    attachPageEvents();

  } catch (err) {
    showToast(err.message || 'Failed to load status pages', 'error');
  }
}

function attachPageEvents() {
  document.querySelectorAll('.ann-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      selectedSpId = btn.getAttribute('data-id');
      openModal('ann-modal');
    });
  });

  document.querySelectorAll('.pub-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      try {
        await api.post(`/status-pages/${id}/publish`);
        showToast('Status page published', 'success');
        loadPages();
      } catch (e) {
        showToast(e.message || 'Failed to publish', 'error');
      }
    });
  });

  document.querySelectorAll('.unpub-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      try {
        await api.post(`/status-pages/${id}/unpublish`);
        showToast('Status page unpublished', 'info');
        loadPages();
      } catch (e) {
        showToast(e.message || 'Failed to unpublish', 'error');
      }
    });
  });

  document.querySelectorAll('.del-sp-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      const confirmed = await showConfirmModal({
        title: 'Delete Status Page?',
        message: 'Are you sure you want to permanently delete this status page? Its public live URL will immediately stop serving.',
        confirmText: 'Delete Page',
        confirmColor: 'danger'
      });
      if (confirmed) {
        try {
          await api.del(`/status-pages/${id}`);
          showToast('Status page deleted', 'success');
          loadPages();
        } catch (e) {
          showToast(e.message || 'Failed to delete', 'error');
        }
      }
    });
  });
}

// Populate available monitors in modal
async function populateMonitorCheckboxes() {
  const container = document.getElementById('sp-monitors-list');
  try {
    const data = await api.get('/monitors', { size: 50 });
    const monitors = data.content || [];

    if (monitors.length === 0) {
      container.innerHTML = '<div style="color:var(--text-dim); font-size:12px;">No monitors available. Create a monitor first.</div>';
      return;
    }

    container.innerHTML = monitors.map(m => `
      <label style="display:flex; align-items:center; gap:8px; padding:4px 0; font-size:13px; cursor:pointer;">
        <input type="checkbox" name="monitorId" value="${m.id}" checked>
        <span>${escapeHtml(m.name)}</span>
        <span style="font-size:11px; color:var(--text-dim);">(${m.type})</span>
      </label>
    `).join('');
  } catch (e) {
    console.warn(e);
  }
}

// Modals
document.getElementById('create-sp-btn')?.addEventListener('click', () => {
  populateMonitorCheckboxes();
  openModal('sp-modal');
});

document.getElementById('close-sp-modal')?.addEventListener('click', () => closeModal('sp-modal'));
document.getElementById('cancel-sp-btn')?.addEventListener('click', () => closeModal('sp-modal'));

document.getElementById('sp-form')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const selectedMonitors = Array.from(document.querySelectorAll('#sp-monitors-list input[name="monitorId"]:checked'))
    .map(cb => parseInt(cb.value));

  const payload = {
    title: document.getElementById('sp-title').value.trim(),
    slug: document.getElementById('sp-slug').value.trim().toLowerCase(),
    description: document.getElementById('sp-desc').value.trim(),
    published: document.getElementById('sp-published').checked,
    monitorIds: selectedMonitors
  };

  try {
    await api.post('/status-pages', payload);
    showToast('Status page created successfully', 'success');
    closeModal('sp-modal');
    document.getElementById('sp-form').reset();
    loadPages();
  } catch (err) {
    showToast(err.message || 'Failed to create status page', 'error');
  }
});

// Announcement Form
document.getElementById('close-ann-modal')?.addEventListener('click', () => closeModal('ann-modal'));
document.getElementById('cancel-ann-btn')?.addEventListener('click', () => closeModal('ann-modal'));

document.getElementById('ann-form')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!selectedSpId) return;

  const payload = {
    title: document.getElementById('ann-title').value.trim(),
    message: document.getElementById('ann-msg').value.trim()
  };

  try {
    await api.post(`/status-pages/${selectedSpId}/announcements`, payload);
    showToast('Announcement posted', 'success');
    closeModal('ann-modal');
    document.getElementById('ann-form').reset();
  } catch (err) {
    showToast(err.message || 'Failed to post announcement', 'error');
  }
});

loadPages();
