import { api } from './api.js';
import { auth } from './auth.js';
import { renderLayout, showToast, showConfirmModal, formatDate, escapeHtml, openModal, closeModal } from './common.js';

auth.requireAuth();
renderLayout('Teams');

let selectedTeamId = null;

async function loadTeams() {
  const tbody = document.getElementById('teams-tbody');
  try {
    const teams = await api.get('/teams');
    if (!teams || teams.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5" style="text-align:center; color:var(--text-dim); padding:24px;">No teams created yet. Click "+ Create Team" to get started.</td></tr>';
      return;
    }

    tbody.innerHTML = teams.map(t => `
      <tr>
        <td><strong style="font-size:15px; color:var(--text-main);">${escapeHtml(t.name)}</strong></td>
        <td>${escapeHtml(t.ownerName)}</td>
        <td><span class="badge badge-paused">${t.memberCount} Members</span></td>
        <td style="font-size:12px; color:var(--text-muted);">${formatDate(t.createdAt)}</td>
        <td style="text-align: right;">
          <button class="btn btn-secondary btn-sm members-btn" data-id="${t.id}" data-name="${escapeHtml(t.name)}">Manage Members</button>
          <button class="btn btn-danger btn-sm del-team-btn" data-id="${t.id}">&times;</button>
        </td>
      </tr>
    `).join('');

    attachTeamEvents();

  } catch (err) {
    showToast(err.message || 'Failed to load teams', 'error');
  }
}

function attachTeamEvents() {
  document.querySelectorAll('.members-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      selectedTeamId = btn.getAttribute('data-id');
      const teamName = btn.getAttribute('data-name');
      openMembersModal(selectedTeamId, teamName);
    });
  });

  document.querySelectorAll('.del-team-btn').forEach(btn => {
    btn.addEventListener('click', async () => {
      const id = btn.getAttribute('data-id');
      const confirmed = await showConfirmModal({
        title: 'Delete Team?',
        message: 'Are you sure you want to delete this team? All member permissions and assignments will be revoked.',
        confirmText: 'Delete Team',
        confirmColor: 'danger'
      });
      if (confirmed) {
        try {
          await api.del(`/teams/${id}`);
          showToast('Team deleted', 'success');
          loadTeams();
        } catch (e) {
          showToast(e.message || 'Failed to delete team', 'error');
        }
      }
    });
  });
}

async function openMembersModal(teamId, teamName) {
  document.getElementById('members-modal-title').innerText = `Team Members — ${teamName}`;
  openModal('members-modal');
  loadMembers(teamId);
}

async function loadMembers(teamId) {
  const tbody = document.getElementById('members-tbody');
  try {
    const members = await api.get(`/teams/${teamId}/members`);
    if (!members || members.length === 0) {
      tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:var(--text-dim); padding:16px;">No members found.</td></tr>';
      return;
    }

    tbody.innerHTML = members.map(m => `
      <tr>
        <td>
          <strong>${escapeHtml(m.userName)}</strong>
          <span style="display:block; font-size:12px; color:var(--text-dim);">${escapeHtml(m.userEmail)}</span>
        </td>
        <td><span class="badge ${m.role === 'OWNER' ? 'badge-up' : (m.role === 'ADMIN' ? 'badge-degraded' : 'badge-paused')}">${m.role}</span></td>
        <td style="font-size:12px; color:var(--text-muted);">${formatDate(m.joinedAt)}</td>
        <td style="text-align:right;">
          ${m.role !== 'OWNER' ? `
            <button class="btn btn-danger btn-sm remove-member-btn" data-userid="${m.userId}">Remove</button>
          ` : '<span style="font-size:12px; color:var(--text-dim);">Owner</span>'}
        </td>
      </tr>
    `).join('');

    tbody.querySelectorAll('.remove-member-btn').forEach(btn => {
      btn.addEventListener('click', async () => {
        const userId = btn.getAttribute('data-userid');
        const confirmed = await showConfirmModal({
          title: 'Remove Team Member?',
          message: 'Are you sure you want to remove this member from the team?',
          confirmText: 'Remove Member',
          confirmColor: 'danger'
        });
        if (confirmed) {
          try {
            await api.del(`/teams/${teamId}/members/${userId}`);
            showToast('Member removed', 'success');
            loadMembers(teamId);
            loadTeams();
          } catch (e) {
            showToast(e.message || 'Failed to remove member', 'error');
          }
        }
      });
    });

  } catch (err) {
    showToast(err.message || 'Failed to load members', 'error');
  }
}

// Create Team Modal
document.getElementById('create-team-btn')?.addEventListener('click', () => openModal('team-modal'));
document.getElementById('close-team-modal')?.addEventListener('click', () => closeModal('team-modal'));
document.getElementById('cancel-team-btn')?.addEventListener('click', () => closeModal('team-modal'));

document.getElementById('team-form')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const name = document.getElementById('team-name').value.trim();
  try {
    await api.post('/teams', { name });
    showToast('Team created successfully', 'success');
    closeModal('team-modal');
    document.getElementById('team-form').reset();
    loadTeams();
  } catch (err) {
    showToast(err.message || 'Failed to create team', 'error');
  }
});

// Invite Member Form
document.getElementById('invite-form')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!selectedTeamId) return;

  const email = document.getElementById('invite-email').value.trim();
  const role = document.getElementById('invite-role').value;

  try {
    await api.post(`/teams/${selectedTeamId}/invitations`, { email, role });
    showToast(`Invitation sent to ${email}`, 'success');
    document.getElementById('invite-email').value = '';
  } catch (err) {
    showToast(err.message || 'Failed to send invite', 'error');
  }
});

document.getElementById('close-members-modal')?.addEventListener('click', () => closeModal('members-modal'));
document.getElementById('close-members-btn')?.addEventListener('click', () => closeModal('members-modal'));

loadTeams();
