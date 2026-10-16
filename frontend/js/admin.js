import { api } from './api.js';
import { auth } from './auth.js';
import { renderLayout, showToast, showConfirmModal, showPromptModal, formatDate, escapeHtml } from './common.js';

auth.requireAdmin();
renderLayout('Admin');

let searchUserQuery = '';

async function loadAllAdminData() {
  await Promise.all([
    loadOverview(),
    loadUsers(),
    loadSettings(),
    loadAuditLogs()
  ]);
}

async function loadOverview() {
  try {
    const data = await api.get('/admin/overview');
    document.getElementById('adm-users').innerText = data.totalUsers;
    document.getElementById('adm-active-users').innerText = `${data.activeUsers} Active accounts`;
    document.getElementById('adm-monitors').innerText = data.totalMonitors;
    document.getElementById('adm-enabled-monitors').innerText = `${data.enabledMonitors} Enabled across platform`;
    document.getElementById('adm-checks').innerText = data.totalCheckResults.toLocaleString();
    document.getElementById('adm-incidents').innerText = data.openIncidents;
    document.getElementById('adm-failed-notifs').innerText = `${data.failedNotifications} Failed notifications`;
  } catch (err) {
    showToast(err.message || 'Failed to load admin overview', 'error');
  }
}

async function loadUsers() {
  const tbody = document.getElementById('users-tbody');
  try {
    const params = { size: 30 };
    if (searchUserQuery) params.q = searchUserQuery;

    const data = await api.get('/admin/users', params);
    const users = data.content || [];

    if (users.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:var(--text-dim); padding:24px;">No users found matching query.</td></tr>';
      return;
    }

    tbody.innerHTML = users.map(u => `
      <tr>
        <td>
          <strong style="color:var(--text-main); font-size:14px;">${escapeHtml(u.name)}</strong>
          <span style="display:block; font-size:12px; color:var(--text-dim);">${escapeHtml(u.email)}</span>
        </td>
        <td>
          <span class="badge ${u.role === 'ADMIN' ? 'badge-degraded' : 'badge-paused'}">${u.role}</span>
        </td>
        <td>
          ${u.enabled 
            ? '<span class="badge badge-up"><span class="badge-dot"></span>ACTIVE</span>' 
            : '<span class="badge badge-down"><span class="badge-dot"></span>SUSPENDED</span>'}
        </td>
        <td>${u.emailVerified ? '✅ Yes' : '❌ No'}</td>
        <td style="font-size:12px; color:var(--text-muted);">${formatDate(u.createdAt)}</td>
        <td style="text-align: right;">
          ${u.enabled ? `
            <button class="btn btn-secondary btn-sm suspend-user-btn" data-id="${u.id}">Suspend</button>
          ` : `
            <button class="btn btn-primary btn-sm reactivate-user-btn" data-id="${u.id}">Reactivate</button>
          `}
          ${u.role === 'USER' ? `
            <button class="btn btn-secondary btn-sm promote-btn" data-id="${u.id}" title="Grant Admin">Make Admin</button>
          ` : `
            <button class="btn btn-secondary btn-sm demote-btn" data-id="${u.id}" title="Remove Admin">Demote</button>
          `}
        </td>
      </tr>
    `).join('');

    attachUserActions();

  } catch (err) {
    console.warn(err);
  }
}

function attachUserActions() {
  document.querySelectorAll('.suspend-user-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      try {
        await api.patch(`/admin/users/${id}/status`, { enabled: false });
        showToast('User account suspended', 'info');
        loadUsers();
      } catch (e) {
        showToast(e.message || 'Error', 'error');
      }
    });
  });

  document.querySelectorAll('.reactivate-user-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      try {
        await api.patch(`/admin/users/${id}/status`, { enabled: true });
        showToast('User account reactivated', 'success');
        loadUsers();
      } catch (e) {
        showToast(e.message || 'Error', 'error');
      }
    });
  });

  document.querySelectorAll('.promote-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      const confirmed = await showConfirmModal({
        title: 'Promote to Administrator?',
        message: 'Are you sure you want to promote this user to system Administrator? They will have full administrative permissions.',
        confirmText: 'Promote User',
        confirmColor: 'primary'
      });
      if (confirmed) {
        try {
          await api.patch(`/admin/users/${id}/role`, { role: 'ADMIN' });
          showToast('User promoted to ADMIN', 'success');
          loadUsers();
        } catch (e) {
          showToast(e.message || 'Error', 'error');
        }
      }
    });
  });

  document.querySelectorAll('.demote-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      const confirmed = await showConfirmModal({
        title: 'Demote Administrator?',
        message: 'Are you sure you want to demote this administrator to regular USER? Their administrative permissions will be revoked.',
        confirmText: 'Demote to User',
        confirmColor: 'danger'
      });
      if (confirmed) {
        try {
          await api.patch(`/admin/users/${id}/role`, { role: 'USER' });
          showToast('User demoted to USER', 'info');
          loadUsers();
        } catch (e) {
          showToast(e.message || 'Error', 'error');
        }
      }
    });
  });
}

async function loadSettings() {
  const tbody = document.getElementById('settings-tbody');
  try {
    const settings = await api.get('/admin/settings');
    if (!settings || settings.length === 0) {
      tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:var(--text-dim); padding:16px;">No platform settings found.</td></tr>';
      return;
    }

    tbody.innerHTML = settings.map(s => `
      <tr>
        <td><code style="color:var(--accent-cyan); font-weight:600;">${escapeHtml(s.settingKey)}</code></td>
        <td><strong>${escapeHtml(s.settingValue)}</strong></td>
        <td style="font-size:12px; color:var(--text-muted);">${formatDate(s.updatedAt)}</td>
        <td style="text-align: right;">
          <button class="btn btn-secondary btn-sm edit-setting-btn" data-key="${escapeHtml(s.settingKey)}" data-val="${escapeHtml(s.settingValue)}">Edit Value</button>
        </td>
      </tr>
    `).join('');

    tbody.querySelectorAll('.edit-setting-btn').forEach(btn => {
      btn.addEventListener('click', async () => {
        const key = btn.getAttribute('data-key');
        const oldVal = btn.getAttribute('data-val');
        const newVal = await showPromptModal({
          title: `Update System Setting`,
          message: `Configure platform parameter value for '${key}':`,
          defaultValue: oldVal,
          confirmText: 'Update Setting'
        });
        if (newVal !== null && newVal.trim() !== '') {
          try {
            await api.put('/admin/settings', { settingKey: key, settingValue: newVal.trim() });
            showToast('Setting updated', 'success');
            loadSettings();
          } catch (e) {
            showToast(e.message || 'Failed to update setting', 'error');
          }
        }
      });
    });

  } catch (e) {
    console.warn(e);
  }
}

async function loadAuditLogs() {
  const tbody = document.getElementById('audit-tbody');
  try {
    const data = await api.get('/admin/audit-logs', { size: 30 });
    const logs = data.content || [];

    if (logs.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:var(--text-dim); padding:24px;">No audit logs recorded yet.</td></tr>';
      return;
    }

    tbody.innerHTML = logs.map(l => `
      <tr>
        <td style="font-size:12px; color:var(--text-muted);">${formatDate(l.createdAt)}</td>
        <td><strong>${escapeHtml(l.actorName || 'System')}</strong></td>
        <td><span class="badge badge-paused">${escapeHtml(l.action)}</span></td>
        <td>${escapeHtml(l.resourceType || '—')} #${escapeHtml(l.resourceId || '—')}</td>
        <td style="font-size:12px; color:var(--text-dim); max-width:280px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;" title="${escapeHtml(l.details || '')}">
          ${escapeHtml(l.details || '—')}
        </td>
        <td style="font-family:monospace; font-size:12px;">${escapeHtml(l.ipAddress || '—')}</td>
      </tr>
    `).join('');

  } catch (e) {
    console.warn(e);
  }
}

document.getElementById('user-search')?.addEventListener('input', (e) => {
  searchUserQuery = e.target.value.trim();
  loadUsers();
});

document.getElementById('admin-refresh-btn')?.addEventListener('click', () => loadAllAdminData());

loadAllAdminData();
