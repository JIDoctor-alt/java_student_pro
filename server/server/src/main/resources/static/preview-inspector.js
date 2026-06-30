/**
 * 预览页可视化选区脚本（由平台注入到 /preview/** 与 srcdoc 预览）
 */
(function () {
  'use strict'

  var enabled = false
  var selectedEl = null
  var overlay = null
  var label = null

  function ensureOverlay() {
    if (overlay) return
    overlay = document.createElement('div')
    overlay.id = '__code_visual_overlay__'
    overlay.style.cssText =
      'position:fixed;pointer-events:none;z-index:2147483646;display:none;' +
      'outline:2px dashed #52c41a;outline-offset:2px;background:rgba(82,196,26,0.08);'
    label = document.createElement('div')
    label.style.cssText =
      'position:fixed;z-index:2147483647;display:none;padding:2px 8px;' +
      'font:12px/1.4 sans-serif;color:#fff;background:#1677ff;border-radius:4px;pointer-events:none;'
    document.documentElement.appendChild(overlay)
    document.documentElement.appendChild(label)
  }

  function getCssSelector(el) {
    if (!el || el.nodeType !== 1) return ''
    if (el.id) return '#' + CSS.escape(el.id)
    var parts = []
    var cur = el
    while (cur && cur.nodeType === 1 && cur !== document.documentElement) {
      var part = cur.tagName.toLowerCase()
      if (cur.classList && cur.classList.length) {
        part += '.' + Array.from(cur.classList)
          .slice(0, 3)
          .map(function (c) {
            return CSS.escape(c)
          })
          .join('.')
      }
      var parent = cur.parentElement
      if (parent) {
        var siblings = Array.from(parent.children).filter(function (n) {
          return n.tagName === cur.tagName
        })
        if (siblings.length > 1) {
          part += ':nth-of-type(' + (siblings.indexOf(cur) + 1) + ')'
        }
      }
      parts.unshift(part)
      if (parts.length >= 5) break
      cur = cur.parentElement
    }
    return parts.join(' > ')
  }

  function getXPath(el) {
    if (!el || el.nodeType !== 1) return ''
    var segments = []
    var cur = el
    while (cur && cur.nodeType === 1) {
      var index = 1
      var sib = cur.previousElementSibling
      while (sib) {
        if (sib.tagName === cur.tagName) index++
        sib = sib.previousElementSibling
      }
      segments.unshift(cur.tagName.toLowerCase() + '[' + index + ']')
      cur = cur.parentElement
      if (segments.length > 8) break
    }
    return '/' + segments.join('/')
  }

  function getSiblingIndex(el) {
    if (!el || !el.parentElement) return 0
    return Array.from(el.parentElement.children).indexOf(el)
  }

  function readSourceHint(el) {
    var cur = el
    while (cur && cur !== document.documentElement) {
      if (cur.dataset && cur.dataset.codeFile) {
        return {
          sourceFile: cur.dataset.codeFile,
          sourceLine: cur.dataset.codeLine ? parseInt(cur.dataset.codeLine, 10) : undefined,
        }
      }
      var comment = cur.previousSibling
      if (comment && comment.nodeType === 8 && comment.textContent) {
        var m = comment.textContent.match(/@file\s+(\S+)/)
        if (m) return { sourceFile: m[1] }
      }
      cur = cur.parentElement
    }
    return {}
  }

  function buildContext(el) {
    var sourceHint = readSourceHint(el)
    var text = (el.innerText || el.textContent || '').trim().replace(/\s+/g, ' ')
    var outer = el.outerHTML || ''
    if (outer.length > 2048) outer = outer.slice(0, 2048) + '...'
    return {
      elementId: el.dataset && el.dataset.codeId ? el.dataset.codeId : undefined,
      tagName: el.tagName ? el.tagName.toLowerCase() : '',
      cssSelector: getCssSelector(el),
      xpath: getXPath(el),
      siblingIndex: getSiblingIndex(el),
      textPreview: text.length > 200 ? text.slice(0, 200) + '...' : text,
      outerHtmlSnippet: outer,
      sourceFile: sourceHint.sourceFile,
      sourceLine: sourceHint.sourceLine,
      pageUrl: location.href,
      editMode: 'chat',
    }
  }

  function positionOverlay(el) {
    if (!overlay || !label || !el) return
    var rect = el.getBoundingClientRect()
    overlay.style.display = 'block'
    overlay.style.top = rect.top + 'px'
    overlay.style.left = rect.left + 'px'
    overlay.style.width = rect.width + 'px'
    overlay.style.height = rect.height + 'px'
    label.style.display = 'block'
    label.style.top = Math.max(4, rect.top - 26) + 'px'
    label.style.left = Math.max(4, rect.left) + 'px'
    var id = el.dataset && el.dataset.codeId ? ' #' + el.dataset.codeId : ''
    label.textContent = (el.tagName || '').toLowerCase() + id
  }

  function clearSelection() {
    selectedEl = null
    if (overlay) overlay.style.display = 'none'
    if (label) label.style.display = 'none'
    postToParent({ type: 'VISUAL_EDIT_CLEAR' })
  }

  function postToParent(payload) {
    if (!window.parent || window.parent === window) return
    window.parent.postMessage(payload, '*')
  }

  function onClick(e) {
    if (!enabled) return
    var t = e.target
    if (!t || t === overlay || t === label) return
    if (t.id === '__code_visual_overlay__') return
    e.preventDefault()
    e.stopPropagation()
    selectedEl = t
    ensureOverlay()
    positionOverlay(t)
    postToParent({ type: 'VISUAL_EDIT_SELECT', payload: buildContext(t) })
  }

  function onMessage(e) {
    var data = e.data
    if (!data || typeof data.type !== 'string') return
    if (data.type === 'VISUAL_EDIT_MODE') {
      enabled = !!data.enabled
      if (!enabled) clearSelection()
      document.body.style.cursor = enabled ? 'crosshair' : ''
    }
    if (data.type === 'VISUAL_EDIT_HIGHLIGHT' && data.elementId) {
      var el = document.querySelector('[data-code-id="' + CSS.escape(data.elementId) + '"]')
      if (el) {
        selectedEl = el
        ensureOverlay()
        positionOverlay(el)
      }
    }
  }

  function onResize() {
    if (selectedEl) positionOverlay(selectedEl)
  }

  document.addEventListener('click', onClick, true)
  window.addEventListener('message', onMessage)
  window.addEventListener('resize', onResize)
  window.addEventListener('scroll', onResize, true)

  postToParent({ type: 'VISUAL_EDIT_READY' })
})()
