/**
 * FileUpload Manager — Spring MVC
 * Handles drag & drop, file queue, progressive upload with XHR, and UI state.
 */
(function () {
  'use strict';

  // ── DOM References ──────────────────────────────────────────────────────────
  const dropzone     = document.getElementById('dropzone');
  const fileInput    = document.getElementById('fileInput');
  const browseBtn    = document.getElementById('browseBtn');
  const uploadQueue  = document.getElementById('uploadQueue');
  const queueList    = document.getElementById('queueList');
  const queueCount   = document.getElementById('queueCount');
  const uploadAllBtn = document.getElementById('uploadAllBtn');
  const clearQueueBtn= document.getElementById('clearQueueBtn');
  const filesGrid    = document.getElementById('filesGrid');
  const refreshBtn   = document.getElementById('refreshBtn');
  const fileCountBadge = document.getElementById('fileCountBadge');
  const toastContainer = document.getElementById('toastContainer');

  // ── State ───────────────────────────────────────────────────────────────────
  const queue = new Map(); // id → { file, element, status }
  let idCounter = 0;

  // ── Drag & Drop ─────────────────────────────────────────────────────────────

  ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(evt =>
    document.addEventListener(evt, e => e.preventDefault())
  );

  dropzone.addEventListener('dragenter', () => dropzone.classList.add('drag-over'));
  dropzone.addEventListener('dragover',  () => dropzone.classList.add('drag-over'));
  dropzone.addEventListener('dragleave', e => {
    if (!dropzone.contains(e.relatedTarget)) dropzone.classList.remove('drag-over');
  });
  dropzone.addEventListener('drop', e => {
    dropzone.classList.remove('drag-over');
    const files = Array.from(e.dataTransfer.files);
    if (files.length) addToQueue(files);
  });

  dropzone.addEventListener('click', e => {
    if (e.target !== browseBtn) fileInput.click();
  });
  browseBtn.addEventListener('click', e => {
    e.stopPropagation();
    fileInput.click();
  });
  fileInput.addEventListener('change', () => {
    if (fileInput.files.length) {
      addToQueue(Array.from(fileInput.files));
      fileInput.value = '';
    }
  });

  // ── Queue Management ────────────────────────────────────────────────────────

  function addToQueue(files) {
    files.forEach(file => {
      const id = ++idCounter;
      const el = createQueueItem(id, file);
      queueList.appendChild(el);
      queue.set(id, { file, element: el, status: 'pending' });
    });
    updateQueueUI();
  }

  function createQueueItem(id, file) {
    const div = document.createElement('div');
    div.className = 'queue-item';
    div.dataset.id = id;
    div.innerHTML = `
      <div class="queue-item-icon">${getFileEmoji(file.name)}</div>
      <div class="queue-item-info">
        <div class="queue-item-name" title="${escHtml(file.name)}">${escHtml(file.name)}</div>
        <div class="queue-item-meta">${formatSize(file.size)} · ${getExtension(file.name).toUpperCase() || 'FILE'}</div>
        <div class="queue-progress" id="progress-${id}">
          <div class="queue-progress-bar" id="bar-${id}"></div>
        </div>
      </div>
      <div class="queue-item-status">
        <span class="status-icon pending" id="status-${id}">○</span>
        <button class="remove-btn" title="Remove" onclick="removeFromQueue(${id})">✕</button>
      </div>
    `;
    return div;
  }

  window.removeFromQueue = function(id) {
    const item = queue.get(id);
    if (!item) return;
    item.element.remove();
    queue.delete(id);
    updateQueueUI();
  };

  function updateQueueUI() {
    const count = queue.size;
    queueCount.textContent = count;
    uploadQueue.style.display = count > 0 ? 'block' : 'none';
    uploadAllBtn.disabled = count === 0;
  }

  clearQueueBtn.addEventListener('click', () => {
    queue.forEach(item => item.element.remove());
    queue.clear();
    updateQueueUI();
  });

  // ── Upload ──────────────────────────────────────────────────────────────────

  uploadAllBtn.addEventListener('click', () => uploadAll());

  async function uploadAll() {
    const pending = [...queue.entries()].filter(([, v]) => v.status === 'pending');
    if (!pending.length) { showToast('info', 'No files', 'All files already uploaded.'); return; }

    uploadAllBtn.disabled = true;
    uploadAllBtn.textContent = 'Uploading…';

    const results = await Promise.allSettled(pending.map(([id, item]) => uploadFile(id, item)));

    const succeeded = results.filter(r => r.status === 'fulfilled' && r.value).length;
    const failed    = results.length - succeeded;

    if (succeeded) {
      showToast('success', 'Upload complete', `${succeeded} file${succeeded > 1 ? 's' : ''} uploaded successfully.`);
      refreshFileList();
    }
    if (failed) {
      showToast('error', 'Some files failed', `${failed} file${failed > 1 ? 's' : ''} could not be uploaded.`);
    }

    uploadAllBtn.textContent = 'Upload All';
    uploadAllBtn.disabled = queue.size === 0;
  }

  function uploadFile(id, item) {
    return new Promise(resolve => {
      const formData = new FormData();
      formData.append('file', item.file);

      setItemStatus(id, 'uploading');
      showProgress(id);

      const xhr = new XMLHttpRequest();
      xhr.open('POST', '/api/upload', true);

      xhr.upload.addEventListener('progress', e => {
        if (e.lengthComputable) {
          const pct = Math.round((e.loaded / e.total) * 100);
          setProgress(id, pct);
        }
      });

      xhr.addEventListener('load', () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          setItemStatus(id, 'success');
          setProgress(id, 100);
          // Remove from queue after short delay
          setTimeout(() => {
            const entry = queue.get(id);
            if (entry) { entry.element.remove(); queue.delete(id); updateQueueUI(); }
          }, 1500);
          resolve(true);
        } else {
          let msg = 'Upload failed';
          try { msg = JSON.parse(xhr.responseText).message || msg; } catch {}
          setItemStatus(id, 'error', msg);
          resolve(false);
        }
      });

      xhr.addEventListener('error', () => {
        setItemStatus(id, 'error', 'Network error');
        resolve(false);
      });

      xhr.send(formData);
      item.status = 'uploading';
    });
  }

  function setItemStatus(id, status, tooltip = '') {
    const icon = document.getElementById(`status-${id}`);
    if (!icon) return;
    icon.className = `status-icon ${status}`;
    const symbols = { pending: '○', uploading: '◌', success: '✓', error: '✗' };
    icon.textContent = symbols[status] || '?';
    if (tooltip) icon.title = tooltip;
    const item = queue.get(id);
    if (item) item.status = status;
  }

  function showProgress(id) {
    const bar = document.getElementById(`progress-${id}`);
    if (bar) bar.style.display = 'block';
  }

  function setProgress(id, pct) {
    const bar = document.getElementById(`bar-${id}`);
    if (bar) bar.style.width = pct + '%';
  }

  // ── File List (dynamic) ─────────────────────────────────────────────────────

  refreshBtn.addEventListener('click', refreshFileList);

  function refreshFileList() {
    fetch('/api/files')
      .then(r => r.json())
      .then(files => {
        renderFileList(files);
        fileCountBadge.textContent = files.length;
      })
      .catch(() => showToast('error', 'Refresh failed', 'Could not load file list.'));
  }

  function renderFileList(files) {
    const emptyState = document.getElementById('emptyState');
    // Remove all file cards
    filesGrid.querySelectorAll('.file-card').forEach(el => el.remove());

    if (!files.length) {
      if (!emptyState) {
        const div = document.createElement('div');
        div.id = 'emptyState';
        div.className = 'empty-state';
        div.innerHTML = '<div class="empty-icon">📂</div><p>No files uploaded yet</p>';
        filesGrid.appendChild(div);
      }
      return;
    }

    if (emptyState) emptyState.remove();

    files.forEach(file => {
      filesGrid.appendChild(createFileCard(file));
    });
  }

  function createFileCard(file) {
    const div = document.createElement('div');
    div.className = 'file-card';
    div.dataset.id = file.id;

    const emoji = categoryEmoji(file.fileCategory);
    const iconClass = file.fileCategory === 'IMAGE' ? 'icon-image'
                    : file.fileCategory === 'DOCUMENT' ? 'icon-doc' : 'icon-other';

    div.innerHTML = `
      <div class="file-card-icon ${iconClass}">${emoji}</div>
      <div class="file-card-info">
        <p class="file-name" title="${escHtml(file.originalName)}">${escHtml(file.originalName)}</p>
        <p class="file-meta">
          <span>${escHtml(file.formattedSize)}</span>
          <span class="dot">·</span>
          <span>${escHtml(file.formattedUploadDate)}</span>
        </p>
      </div>
      <div class="file-card-actions">
        <a href="/api/files/${file.id}/download" class="action-btn" title="Download">⬇</a>
        <button class="action-btn action-delete" title="Delete" onclick="deleteFile('${file.id}')">✕</button>
      </div>
    `;
    return div;
  }

  window.deleteFile = function(fileId) {
    if (!confirm('Delete this file?')) return;
    fetch(`/api/files/${fileId}`, { method: 'DELETE' })
      .then(r => {
        if (r.ok) {
          const el = filesGrid.querySelector(`[data-id="${fileId}"]`);
          if (el) el.remove();
          const count = parseInt(fileCountBadge.textContent || '0') - 1;
          fileCountBadge.textContent = Math.max(0, count);
          if (filesGrid.querySelectorAll('.file-card').length === 0) {
            const div = document.createElement('div');
            div.id = 'emptyState'; div.className = 'empty-state';
            div.innerHTML = '<div class="empty-icon">📂</div><p>No files uploaded yet</p>';
            filesGrid.appendChild(div);
          }
          showToast('success', 'File deleted', 'File removed successfully.');
        } else {
          showToast('error', 'Delete failed', 'Could not delete the file.');
        }
      });
  };

  // ── Toast ────────────────────────────────────────────────────────────────────

  function showToast(type, title, message, duration = 4000) {
    const icons = { success: '✓', error: '✗', info: 'ℹ' };
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
      <span class="toast-icon">${icons[type] || 'ℹ'}</span>
      <div class="toast-body">
        <div class="toast-title">${escHtml(title)}</div>
        ${message ? `<div class="toast-message">${escHtml(message)}</div>` : ''}
      </div>
    `;
    toastContainer.appendChild(toast);
    setTimeout(() => {
      toast.classList.add('removing');
      setTimeout(() => toast.remove(), 300);
    }, duration);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────

  function formatSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  function getExtension(filename) {
    return filename.split('.').pop() || '';
  }

  function getFileEmoji(filename) {
    const ext = getExtension(filename).toLowerCase();
    const images = ['jpg','jpeg','png','gif','bmp','webp','svg'];
    const docs   = ['pdf','doc','docx','xls','xlsx','ppt','pptx','txt','csv'];
    const archives = ['zip','rar','7z','tar','gz'];
    const videos = ['mp4','avi','mkv','mov'];
    const audio  = ['mp3','wav','flac','aac'];
    if (images.includes(ext))   return '🖼';
    if (docs.includes(ext))     return '📄';
    if (archives.includes(ext)) return '🗜';
    if (videos.includes(ext))   return '🎬';
    if (audio.includes(ext))    return '🎵';
    return '📁';
  }

  function categoryEmoji(cat) {
    const map = { IMAGE: '🖼', DOCUMENT: '📄', ARCHIVE: '🗜', VIDEO: '🎬', AUDIO: '🎵', OTHER: '📁' };
    return map[cat] || '📁';
  }

  function escHtml(str) {
    if (!str) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

})();
