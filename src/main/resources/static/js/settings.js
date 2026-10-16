import { api } from './api.js';
import { auth } from './auth.js';
import { renderLayout, showToast, formatDate } from './common.js';

auth.requireAuth();
renderLayout('Settings');

async function loadProfile() {
  try {
    const user = await api.get('/auth/me');
    document.getElementById('prof-email').value = user.email;
    document.getElementById('prof-name').value = user.name;
    document.getElementById('sec-role').innerText = user.role;
    document.getElementById('sec-verified').innerText = user.emailVerified ? 'Verified' : 'Pending Verification';
    document.getElementById('sec-verified').style.color = user.emailVerified ? 'var(--color-up)' : 'var(--color-degraded)';
    document.getElementById('sec-created').innerText = formatDate(user.createdAt);
  } catch (err) {
    showToast(err.message || 'Failed to load user profile', 'error');
  }
}

document.getElementById('profile-form')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const name = document.getElementById('prof-name').value.trim();
  const btn = document.getElementById('save-profile-btn');
  btn.disabled = true;

  try {
    const updated = await api.patch('/users/me', { name });
    localStorage.setItem('pulsewatch_user', JSON.stringify(updated));
    showToast('Profile updated successfully', 'success');
    renderLayout('Settings');
  } catch (err) {
    showToast(err.message || 'Failed to update profile', 'error');
  } finally {
    btn.disabled = false;
  }
});

document.getElementById('password-form')?.addEventListener('submit', async (e) => {
  e.preventDefault();
  const currentPassword = document.getElementById('curr-password').value;
  const newPassword = document.getElementById('new-password').value;
  const btn = document.getElementById('save-pwd-btn');
  btn.disabled = true;

  try {
    await api.patch('/users/me/password', { currentPassword, newPassword });
    showToast('Password changed successfully. Security notification sent to your email.', 'success');
    document.getElementById('password-form').reset();
  } catch (err) {
    showToast(err.message || 'Failed to change password', 'error');
  } finally {
    btn.disabled = false;
  }
});

loadProfile();
