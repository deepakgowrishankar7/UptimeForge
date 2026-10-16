/**
 * Common UI and Utility Functions — UptimeForge
 */

import { auth } from './auth.js';

// Global Interval Tracker to prevent runaway background polling across SPA transitions
window.__pageIntervals = window.__pageIntervals || [];
if (!window.__origSetInterval) {
  window.__origSetInterval = window.setInterval;
  window.setInterval = function (fn, delay, ...args) {
    const id = window.__origSetInterval(fn, delay, ...args);
    window.__pageIntervals.push(id);
    return id;
  };
}

export function clearPageIntervals() {
  if (window.__pageIntervals && window.__pageIntervals.length > 0) {
    window.__pageIntervals.forEach(id => clearInterval(id));
    window.__pageIntervals = [];
  }
}

export function renderLayout(activePageTitle = '') {
  const user = auth.getUser() || { name: 'User', email: '', role: 'USER' };
  const isAdmin = user.role === 'ADMIN';

  const sidebarEl = document.getElementById('sidebar-container');
  if (sidebarEl) {
    // If sidebar is already rendered, smoothly update active link state only
    const existingSidebar = sidebarEl.querySelector('.sidebar');
    if (existingSidebar) {
      existingSidebar.querySelectorAll('.nav-link').forEach(link => {
        const text = link.textContent.trim().toLowerCase();
        if (text.includes(activePageTitle.toLowerCase())) {
          link.classList.add('active');
        } else {
          link.classList.remove('active');
        }
      });
      return;
    }

    sidebarEl.innerHTML = `
      <aside class="sidebar">
        <a href="/dashboard.html" class="brand" style="text-decoration:none; display:flex; align-items:center; gap:12px;">
          <img src="/assets/logo.png" alt="UptimeForge Logo" class="brand-logo-img" style="width:34px; height:34px; object-fit:contain; border-radius:8px;">
          <div class="brand-title">Uptime<span>Forge</span></div>
        </a>
        <ul class="nav-menu">
          <li class="nav-item">
            <a href="/dashboard.html" class="nav-link ${activePageTitle === 'Dashboard' ? 'active' : ''}">
              <span class="nav-icon"><svg class="nav-svg-icon" viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect><rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect></svg></span>
              Dashboard
            </a>
          </li>
          <li class="nav-item">
            <a href="/monitors.html" class="nav-link ${activePageTitle === 'Monitors' ? 'active' : ''}">
              <span class="nav-icon"><svg class="nav-svg-icon" viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline></svg></span>
              Monitors
            </a>
          </li>
          <li class="nav-item">
            <a href="/incidents.html" class="nav-link ${activePageTitle === 'Incidents' ? 'active' : ''}">
              <span class="nav-icon"><svg class="nav-svg-icon" viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg></span>
              Incidents
            </a>
          </li>
          <li class="nav-item">
            <a href="/analytics.html" class="nav-link ${activePageTitle === 'Analytics' ? 'active' : ''}">
              <span class="nav-icon"><svg class="nav-svg-icon" viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg></span>
              Analytics
            </a>
          </li>
          <li class="nav-item">
            <a href="/notifications.html" class="nav-link ${activePageTitle === 'Alerts' ? 'active' : ''}">
              <span class="nav-icon"><svg class="nav-svg-icon" viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg></span>
              Alerts & Logs
            </a>
          </li>
          <li class="nav-item">
            <a href="/teams.html" class="nav-link ${activePageTitle === 'Teams' ? 'active' : ''}">
              <span class="nav-icon"><svg class="nav-svg-icon" viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg></span>
              Teams
            </a>
          </li>
          <li class="nav-item">
            <a href="/status-pages.html" class="nav-link ${activePageTitle === 'Status Pages' ? 'active' : ''}">
              <span class="nav-icon"><svg class="nav-svg-icon" viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M4.93 4.93a10 10 0 0 1 14.14 0"></path><path d="M7.76 7.76a6 6 0 0 1 8.48 0"></path><circle cx="12" cy="12" r="2"></circle><path d="M12 14v7"></path></svg></span>
              Status Pages
            </a>
          </li>
          ${isAdmin ? `
          <li class="nav-item" style="margin-top: 14px; padding-top: 10px; border-top: 1px solid var(--border-subtle);">
            <a href="/admin.html" class="nav-link ${activePageTitle === 'Admin' ? 'active' : ''}">
              <span class="nav-icon"><svg class="nav-svg-icon" viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg></span>
              Admin Console
            </a>
          </li>` : ''}
          <li class="nav-item">
            <a href="/settings.html" class="nav-link ${activePageTitle === 'Settings' ? 'active' : ''}">
              <span class="nav-icon"><svg class="nav-svg-icon" viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg></span>
              Settings
            </a>
          </li>
        </ul>
        <div class="sidebar-footer">
          <div class="user-badge">
            <div class="user-avatar">${user.name.charAt(0).toUpperCase()}</div>
            <div class="user-info">
              <div class="user-name">${escapeHtml(user.name)}</div>
              <div class="user-role">${user.role}</div>
            </div>
            <button id="logout-btn" title="Logout" style="background:none;border:none;color:var(--text-muted);cursor:pointer;display:flex;align-items:center;padding:4px;" aria-label="Sign out">
              <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path><polyline points="16 17 21 12 16 7"></polyline><line x1="21" y1="12" x2="9" y2="12"></line></svg>
            </button>
          </div>
        </div>
      </aside>
    `;

    document.getElementById('logout-btn')?.addEventListener('click', () => auth.logout());
  }

  // Create toast container if not already in document
  if (!document.getElementById('toast-container')) {
    const toastContainer = document.createElement('div');
    toastContainer.id = 'toast-container';
    toastContainer.className = 'toast-container';
    document.body.appendChild(toastContainer);
  }
}

/**
 * In-App Confirmation Modal returning a Promise<boolean>
 * Replaces native browser confirm() dialog with high-end animated modal
 */
export function showConfirmModal({
  title = 'Please Confirm',
  message = 'Are you sure you want to proceed?',
  confirmText = 'Confirm',
  cancelText = 'Cancel',
  confirmColor = 'primary' // 'primary' | 'danger'
} = {}) {
  return new Promise((resolve) => {
    document.querySelectorAll('.app-modal-dialog-backdrop').forEach(el => el.remove());

    const backdrop = document.createElement('div');
    backdrop.className = 'app-modal-dialog-backdrop';
    backdrop.style.cssText = `
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 42, 0.65);
      backdrop-filter: blur(4px);
      -webkit-backdrop-filter: blur(4px);
      z-index: 10000;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 16px;
      animation: appFadeIn 0.15s ease-out;
    `;

    const isDanger = confirmColor === 'danger';
    const btnClass = isDanger ? 'btn-danger' : 'btn-primary';
    const iconSvg = isDanger ? `
      <div style="width:46px; height:46px; border-radius:50%; background:#FEF2F2; display:flex; align-items:center; justify-content:center; color:#DC2626; margin-bottom:14px; border:1px solid #FEE2E2;">
        <svg viewBox="0 0 24 24" width="22" height="22" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>
      </div>
    ` : `
      <div style="width:46px; height:46px; border-radius:50%; background:#EFF6FF; display:flex; align-items:center; justify-content:center; color:#2563EB; margin-bottom:14px; border:1px solid #DBEAFE;">
        <svg viewBox="0 0 24 24" width="22" height="22" stroke="currentColor" stroke-width="2.2" fill="none" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
      </div>
    `;

    backdrop.innerHTML = `
      <div class="app-modal-dialog" style="
        background: #FFFFFF;
        border: 1px solid var(--border-subtle, #E2E8F0);
        border-radius: 14px;
        box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.18), 0 10px 10px -5px rgba(0, 0, 0, 0.08);
        width: 100%;
        max-width: 440px;
        padding: 24px;
        animation: appScaleUp 0.18s ease-out;
        box-sizing: border-box;
      ">
        ${iconSvg}
        <h3 style="margin: 0 0 8px 0; font-size: 18px; font-weight: 700; color: #0F172A;">${escapeHtml(title)}</h3>
        <p style="margin: 0 0 22px 0; font-size: 14px; color: #64748B; line-height: 1.55;">${escapeHtml(message)}</p>
        <div style="display: flex; gap: 10px; justify-content: flex-end;">
          <button type="button" class="btn btn-secondary cancel-dialog-btn" style="min-width: 88px; padding: 8px 16px; font-size: 13.5px;">${escapeHtml(cancelText)}</button>
          <button type="button" class="btn ${btnClass} confirm-dialog-btn" style="min-width: 88px; padding: 8px 18px; font-size: 13.5px;">${escapeHtml(confirmText)}</button>
        </div>
      </div>
    `;

    const closeDialog = (val) => {
      backdrop.style.opacity = '0';
      backdrop.style.transition = 'opacity 0.12s ease';
      setTimeout(() => backdrop.remove(), 120);
      document.removeEventListener('keydown', onKeyDown);
      resolve(val);
    };

    const onKeyDown = (e) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        closeDialog(false);
      } else if (e.key === 'Enter') {
        e.preventDefault();
        closeDialog(true);
      }
    };

    backdrop.querySelector('.cancel-dialog-btn').addEventListener('click', () => closeDialog(false));
    backdrop.querySelector('.confirm-dialog-btn').addEventListener('click', () => closeDialog(true));
    backdrop.addEventListener('click', (e) => {
      if (e.target === backdrop) closeDialog(false);
    });

    document.addEventListener('keydown', onKeyDown);
    document.body.appendChild(backdrop);
    backdrop.querySelector('.confirm-dialog-btn').focus();
  });
}

/**
 * In-App Prompt Modal returning a Promise<string | null>
 * Replaces native browser prompt() dialog with high-end animated modal
 */
export function showPromptModal({
  title = 'Input Required',
  message = '',
  defaultValue = '',
  placeholder = '',
  confirmText = 'Save',
  cancelText = 'Cancel',
  inputType = 'text'
} = {}) {
  return new Promise((resolve) => {
    document.querySelectorAll('.app-modal-dialog-backdrop').forEach(el => el.remove());

    const backdrop = document.createElement('div');
    backdrop.className = 'app-modal-dialog-backdrop';
    backdrop.style.cssText = `
      position: fixed;
      inset: 0;
      background: rgba(15, 23, 42, 0.65);
      backdrop-filter: blur(4px);
      -webkit-backdrop-filter: blur(4px);
      z-index: 10000;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 16px;
      animation: appFadeIn 0.15s ease-out;
    `;

    backdrop.innerHTML = `
      <form class="app-modal-dialog" style="
        background: #FFFFFF;
        border: 1px solid var(--border-subtle, #E2E8F0);
        border-radius: 14px;
        box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.18), 0 10px 10px -5px rgba(0, 0, 0, 0.08);
        width: 100%;
        max-width: 440px;
        padding: 24px;
        animation: appScaleUp 0.18s ease-out;
        box-sizing: border-box;
      ">
        <h3 style="margin: 0 0 8px 0; font-size: 18px; font-weight: 700; color: #0F172A;">${escapeHtml(title)}</h3>
        ${message ? `<p style="margin: 0 0 16px 0; font-size: 14px; color: #64748B; line-height: 1.55;">${escapeHtml(message)}</p>` : ''}
        <div style="margin-bottom: 20px;">
          <input type="${escapeHtml(inputType)}" class="form-control prompt-input" value="${escapeHtml(defaultValue)}" placeholder="${escapeHtml(placeholder)}" style="width: 100%; padding: 10px 14px; font-size: 14px; border: 1.5px solid var(--border-subtle, #CBD5E1); border-radius: 8px; box-sizing: border-box;" required />
        </div>
        <div style="display: flex; gap: 10px; justify-content: flex-end;">
          <button type="button" class="btn btn-secondary cancel-dialog-btn" style="min-width: 88px; padding: 8px 16px; font-size: 13.5px;">${escapeHtml(cancelText)}</button>
          <button type="submit" class="btn btn-primary confirm-dialog-btn" style="min-width: 88px; padding: 8px 18px; font-size: 13.5px;">${escapeHtml(confirmText)}</button>
        </div>
      </form>
    `;

    const input = backdrop.querySelector('.prompt-input');

    const closeDialog = (val) => {
      backdrop.style.opacity = '0';
      backdrop.style.transition = 'opacity 0.12s ease';
      setTimeout(() => backdrop.remove(), 120);
      document.removeEventListener('keydown', onKeyDown);
      resolve(val);
    };

    const onKeyDown = (e) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        closeDialog(null);
      }
    };

    backdrop.querySelector('.cancel-dialog-btn').addEventListener('click', () => closeDialog(null));
    backdrop.querySelector('form').addEventListener('submit', (e) => {
      e.preventDefault();
      closeDialog(input.value);
    });
    backdrop.addEventListener('click', (e) => {
      if (e.target === backdrop) closeDialog(null);
    });

    document.addEventListener('keydown', onKeyDown);
    document.body.appendChild(backdrop);
    setTimeout(() => {
      input.focus();
      input.select();
    }, 50);
  });
}

let isNavigating = false;

/**
 * Universal Client-Side SPA Navigation:
 * Swaps view containers and loads scripts without triggering browser tab reload spinner
 * Works across landing, auth, public status, and dashboard sections
 */
export async function spaNavigate(url, pushState = true) {
  if (isNavigating) return;
  const currentPath = window.location.pathname + window.location.search;
  if (url === currentPath) return;

  isNavigating = true;
  try {
    const mainWrapper = document.querySelector('.main-wrapper');
    const isTargetDashboard = !url.includes('login') && !url.includes('register') && !url.includes('index') && !url.includes('public-status');
    const isCurrentDashboard = !!mainWrapper && !!document.querySelector('.sidebar');

    if (isCurrentDashboard && isTargetDashboard) {
      // PATH A: Dashboard to Dashboard section
      // 1. Immediately highlight new active sidebar link for instant 0ms visual responsiveness
      const sidebar = document.querySelector('.sidebar');
      if (sidebar) {
        const urlBase = url.split('?')[0];
        sidebar.querySelectorAll('.nav-link').forEach(link => {
          const linkHref = link.getAttribute('href');
          if (linkHref && linkHref.split('?')[0] === urlBase) {
            link.classList.add('active');
          } else {
            link.classList.remove('active');
          }
        });
      }

      // 2. Fetch target page silently in the background without dimming or flickering the current view
      const res = await fetch(url);
      if (!res.ok) {
        window.location.href = url;
        return;
      }
      const htmlText = await res.text();
      const parser = new DOMParser();
      const newDoc = parser.parseFromString(htmlText, 'text/html');

      clearPageIntervals();
      document.querySelectorAll('.modal-backdrop, .app-modal-dialog-backdrop').forEach(el => el.remove());

      // 3. Instant seamless swap of main container
      const newMain = newDoc.querySelector('.main-wrapper');
      if (mainWrapper && newMain) {
        mainWrapper.innerHTML = newMain.innerHTML;
      }

      const newModals = newDoc.querySelectorAll('.modal-backdrop');
      newModals.forEach(m => document.body.appendChild(m));

      if (newDoc.title) {
        document.title = newDoc.title;
      }

      if (pushState) {
        window.history.pushState({ spa: true, url }, '', url);
      }

      const needsChart = newDoc.querySelector('script[src*="chart.js"]') || url.includes('analytics') || url.includes('dashboard') || url.includes('monitor-details');
      if (needsChart && !window.Chart) {
        await new Promise(resolve => {
          const s = document.createElement('script');
          s.src = 'https://cdn.jsdelivr.net/npm/chart.js';
          s.onload = resolve;
          s.onerror = resolve;
          document.head.appendChild(s);
        });
      }

      const moduleScripts = newDoc.querySelectorAll('script[type="module"]');
      for (const s of moduleScripts) {
        const src = s.getAttribute('src');
        if (src && src.startsWith('/js/')) {
          try {
            await import(src + (src.includes('?') ? '&' : '?') + 't=' + Date.now());
          } catch (err) {
            console.error(`SPA execution error on ${src}:`, err);
          }
        }
      }
    } else {
      // PATH B: Universal Page Switch (index, login, register, public-status, dashboard)
      // Fetch target page silently first
      const res = await fetch(url);
      if (!res.ok) {
        window.location.href = url;
        return;
      }
      const htmlText = await res.text();
      const parser = new DOMParser();
      const newDoc = parser.parseFromString(htmlText, 'text/html');

      clearPageIntervals();
      document.querySelectorAll('.modal-backdrop, .app-modal-dialog-backdrop').forEach(el => el.remove());

      // Sync Head stylesheets
      newDoc.querySelectorAll('link[rel="stylesheet"]').forEach(link => {
        const href = link.getAttribute('href');
        if (href && !document.head.querySelector(`link[href="${href}"]`)) {
          const newLink = document.createElement('link');
          newLink.rel = 'stylesheet';
          newLink.href = href;
          document.head.appendChild(newLink);
        }
      });

      // Transfer target styles
      document.querySelectorAll('style[data-page-style]').forEach(el => el.remove());
      newDoc.querySelectorAll('style').forEach(st => {
        const newSt = document.createElement('style');
        newSt.setAttribute('data-page-style', 'true');
        newSt.textContent = st.textContent;
        document.head.appendChild(newSt);
      });

      // Sync Body Attributes and InnerHTML
      document.body.className = newDoc.body.className;
      document.body.style.cssText = newDoc.body.style.cssText;
      document.body.innerHTML = newDoc.body.innerHTML;

      if (newDoc.title) {
        document.title = newDoc.title;
      }

      if (pushState) {
        window.history.pushState({ spa: true, url }, '', url);
      }
      window.scrollTo(0, 0);

      // Re-execute scripts
      const scripts = newDoc.querySelectorAll('script');
      for (const s of scripts) {
        const src = s.getAttribute('src');
        const type = s.getAttribute('type');
        if (type === 'module') {
          if (src && src.startsWith('/js/')) {
            try {
              await import(src + (src.includes('?') ? '&' : '?') + 't=' + Date.now());
            } catch (err) {
              console.error(`SPA execution error on ${src}:`, err);
            }
          } else if (s.textContent) {
            const inlineMod = document.createElement('script');
            inlineMod.type = 'module';
            inlineMod.textContent = s.textContent;
            document.body.appendChild(inlineMod);
          }
        } else if (src) {
          if (!document.head.querySelector(`script[src="${src}"]`)) {
            await new Promise(resolve => {
              const ext = document.createElement('script');
              ext.src = src;
              ext.onload = resolve;
              ext.onerror = resolve;
              document.head.appendChild(ext);
            });
          }
        } else if (s.textContent) {
          const inlineScript = document.createElement('script');
          inlineScript.textContent = s.textContent;
          document.body.appendChild(inlineScript);
        }
      }
    }
  } catch (err) {
    console.warn('SPA navigation fallback:', err);
    window.location.href = url;
  } finally {
    isNavigating = false;
  }
}

// Global SPA link interceptor & popstate handler
if (typeof window !== 'undefined' && !window.__spaInitialized) {
  window.__spaInitialized = true;

  document.addEventListener('click', (e) => {
    const link = e.target.closest('a');
    if (!link) return;
    const href = link.getAttribute('href');
    if (!href) return;

    if (
      (href.startsWith('/') || href.startsWith('./')) &&
      (href.endsWith('.html') || href.includes('.html?') || href === '/' || href.includes('system-status')) &&
      !href.includes('/swagger-ui') &&
      !link.hasAttribute('target') &&
      !link.hasAttribute('download') &&
      !e.metaKey && !e.ctrlKey && !e.shiftKey
    ) {
      e.preventDefault();
      const resolved = href === '/' ? '/index.html' : href;
      spaNavigate(resolved, true);
    }
  });

  window.addEventListener('popstate', () => {
    const targetUrl = window.location.pathname + window.location.search;
    spaNavigate(targetUrl, false);
  });
}

export function showToast(message, type = 'info') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerText = message;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

export function getStatusBadge(status) {
  const s = (status || 'UNKNOWN').toUpperCase();
  let cls = 'badge-paused';
  if (s === 'UP') cls = 'badge-up';
  else if (s === 'DOWN') cls = 'badge-down';
  else if (s === 'DEGRADED') cls = 'badge-degraded';
  else if (s === 'PAUSED') cls = 'badge-paused';

  return `<span class="badge ${cls}"><span class="badge-dot"></span>${s}</span>`;
}

export function formatDate(isoString) {
  if (!isoString) return 'Never';
  const d = new Date(isoString);
  if (isNaN(d.getTime())) return 'Invalid';
  return d.toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
}

export function formatDuration(seconds) {
  if (!seconds || seconds <= 0) return '0s';
  if (seconds < 60) return `${seconds}s`;
  const m = Math.floor(seconds / 60);
  if (m < 60) return `${m}m ${seconds % 60}s`;
  const h = Math.floor(m / 60);
  return `${h}h ${m % 60}m`;
}

export function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>'"]/g, 
    tag => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[tag] || tag));
}

export function openModal(id) {
  const el = document.getElementById(id);
  if (el) el.classList.add('show');
}

export function closeModal(id) {
  const el = document.getElementById(id);
  if (el) el.classList.remove('show');
}
