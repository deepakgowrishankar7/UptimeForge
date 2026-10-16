import { api } from './api.js';
import { auth } from './auth.js';
import { renderLayout, showToast, showConfirmModal, formatDate, escapeHtml, openModal, closeModal } from './common.js';

auth.requireAuth();
renderLayout('Alerts');

async function loadAll() {
  await Promise.all([
    loadChannels(),
    loadLogs()
  ]);
}

async function loadChannels() {
  const tbody = document.getElementById('channels-tbody');
  try {
    const channels = await api.get('/alerts');
    if (!channels || channels.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:var(--text-dim); padding:24px;">No alert channels configured yet. Click "+ Add Alert Channel" to set up alerts.</td></tr>';
      return;
    }

    tbody.innerHTML = channels.map(c => `
      <tr>
        <td><strong style="color:var(--accent-cyan);">${c.channel}</strong></td>
        <td><span style="font-family:monospace;">${escapeHtml(c.destination)}</span></td>
        <td>${c.cooldownSeconds}s</td>
        <td>${c.notifyOnRecovery ? 'Yes' : 'No'}</td>
        <td>${c.enabled ? '<span class="badge badge-up"><span class="badge-dot"></span>ENABLED</span>' : '<span class="badge badge-paused">DISABLED</span>'}</td>
        <td style="text-align:right;">
          <button class="btn btn-secondary btn-sm test-ch-btn" data-id="${c.id}" title="Send Test Notification">Test Alert</button>
          <button class="btn btn-danger btn-sm del-ch-btn" data-id="${c.id}" title="Delete Channel">&times;</button>
        </td>
      </tr>
    `).join('');

    attachChannelEvents();

  } catch (err) {
    showToast(err.message || 'Failed to load channels', 'error');
  }
}

function attachChannelEvents() {
  document.querySelectorAll('.test-ch-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      btn.disabled = true;
      showToast('Dispatching test notification...', 'info');
      try {
        await api.post(`/alerts/${id}/test`);
        showToast('Test alert dispatched successfully!', 'success');
        loadLogs();
      } catch (e) {
        showToast(e.message || 'Test failed', 'error');
      } finally {
        btn.disabled = false;
      }
    });
  });

  document.querySelectorAll('.del-ch-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      const confirmed = await showConfirmModal({
        title: 'Delete Alert Channel?',
        message: 'Are you sure you want to delete this alert channel? Incident notifications will no longer be dispatched here.',
        confirmText: 'Delete Channel',
        confirmColor: 'danger'
      });
      if (confirmed) {
        try {
          await api.del(`/alerts/${id}`);
          showToast('Channel deleted', 'success');
          loadChannels();
        } catch (e) {
          showToast(e.message || 'Failed to delete', 'error');
        }
      }
    });
  });
}

async function loadLogs() {
  const tbody = document.getElementById('logs-tbody');
  try {
    const data = await api.get('/notifications', { size: 30 });
    const logs = data.content || [];

    if (logs.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; color:var(--text-dim); padding:24px;">No notification logs recorded yet.</td></tr>';
      return;
    }

    tbody.innerHTML = logs.map(l => `
      <tr>
        <td>
          ${l.deliveryStatus === 'DELIVERED' 
            ? '<span class="badge badge-up"><span class="badge-dot"></span>DELIVERED</span>'
            : (l.deliveryStatus === 'PENDING' 
                ? '<span class="badge badge-degraded"><span class="badge-dot"></span>PENDING</span>'
                : (l.deliveryStatus === 'SUPPRESSED_COOLDOWN'
                    ? '<span class="badge badge-paused">COOLDOWN</span>'
                    : '<span class="badge badge-down"><span class="badge-dot"></span>FAILED</span>'))}
        </td>
        <td><strong>${escapeHtml(l.eventType)}</strong></td>
        <td>${escapeHtml(l.channel || '—')}</td>
        <td style="font-family:monospace; font-size:12px;">${escapeHtml(l.destination || '—')}</td>
        <td>${l.attempts}</td>
        <td style="font-size:12px; color:var(--text-muted);">${formatDate(l.lastAttemptAt)}</td>
        <td style="font-size:12px;">${l.deliveredAt ? formatDate(l.deliveredAt) : '—'}</td>
        <td style="font-size:12px; color:${l.errorMessage ? 'var(--color-down)' : 'var(--text-dim)'}; max-width:240px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">
          ${escapeHtml(l.errorMessage || '—')}
        </td>
      </tr>
    `).join('');

  } catch (e) {
    console.warn(e);
  }
}

// Channel type dropdown change
document.getElementById('channel-type')?.addEventListener('change', (e) => {
  const type = e.target.value;
  const label = document.getElementById('dest-label');
  const input = document.getElementById('destination');
  const hint = document.getElementById('dest-hint');

  if (type === 'EMAIL') {
    label.innerText = 'Destination Email Address *';
    input.placeholder = 'ops@company.com';
    hint.innerText = 'Target email address for alerts.';
  } else if (type === 'SLACK') {
    label.innerText = 'Slack Incoming Webhook URL *';
    input.placeholder = 'https://hooks.slack.com/services/...';
    hint.innerText = 'Enter your Slack incoming webhook URL.';
  } else if (type === 'WEBHOOK') {
    label.innerText = 'Webhook URL *';
    input.placeholder = 'https://api.company.com/webhook';
    hint.innerText = 'Target endpoint that will receive POST payloads with event details.';
  }
});

// Modal
document.getElementById('add-channel-btn')?.addEventListener('click', () => openModal('channel-modal'));
document.getElementById('close-channel-modal')?.addEventListener('click', () => closeModal('channel-modal'));
document.getElementById('cancel-channel-btn')?.addEventListener('click', () => closeModal('channel-modal'));

document.getElementById('channel-form')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const payload = {
    channel: document.getElementById('channel-type').value,
    destination: document.getElementById('destination').value.trim(),
    failureThreshold: parseInt(document.getElementById('failure-threshold').value),
    cooldownSeconds: parseInt(document.getElementById('cooldown-sec').value),
    notifyOnRecovery: document.getElementById('notify-recovery').checked,
    enabled: true
  };

  try {
    await api.post('/alerts', payload);
    showToast('Alert channel created successfully', 'success');
    closeModal('channel-modal');
    document.getElementById('channel-form').reset();
    loadChannels();
  } catch (err) {
    showToast(err.message || 'Failed to create channel', 'error');
  }
});

document.getElementById('refresh-logs-btn')?.addEventListener('click', () => loadLogs());

loadAll();
