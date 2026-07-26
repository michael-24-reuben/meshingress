/**
 * Meshingress SVG IconSets
 * Configurable SVG icon generator functions supporting size, color, stroke, classes, accessibility, and DOM element generation.
 */
(function (root, factory) {
  if (typeof define === 'function' && define.amd) {
    define([], factory);
  } else if (typeof module === 'object' && module.exports) {
    module.exports = factory();
  } else {
    root.IconSets = factory();
  }
}(typeof self !== 'undefined' ? self : this, function () {
  'use strict';

  function escapeAttr(val) {
    return String(val).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  function escapeXml(val) {
    return String(val).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  function formatDimension(dim) {
    if (dim === undefined || dim === null) return '24px';
    return typeof dim === 'number' ? `${dim}px` : String(dim);
  }

  /**
   * Helper function to build SVG HTML string or SVGElement DOM node.
   *
   * @param {Function} pathBuilder - Function returning inner SVG paths given resolved color and options.
   * @param {Object} [options] - Configuration options.
   * @param {number|string} [options.size=24] - Width and height shortcut (e.g. 24, "64px", "1em").
   * @param {number|string} [options.width] - Explicit SVG width (overrides size).
   * @param {number|string} [options.height] - Explicit SVG height (overrides size).
   * @param {string} [options.color="currentColor"] - Primary color applied to fill/stroke.
   * @param {string} [options.className=""] - CSS class names attached to <svg>.
   * @param {string|Object} [options.style=""] - Inline styles as string or object { color: 'red' }.
   * @param {string} [options.viewBox="0 0 24 24"] - SVG viewBox coordinates.
   * @param {string} [options.title] - Accessible title element.
   * @param {string} [options.ariaLabel] - Accessible aria-label attribute.
   * @param {string} [options.role] - ARIA role attribute.
   * @param {boolean} [options.asElement=false] - Return SVGElement DOM node if true, string if false.
   * @param {Object} [options.attributes={}] - Custom key-value SVG attributes.
   * @returns {string|SVGElement} SVG markup string or SVGElement DOM node.
   */
  function buildSvg(pathBuilder, options = {}) {
    const size = options.size !== undefined ? options.size : 24;
    const width = options.width !== undefined ? options.width : size;
    const height = options.height !== undefined ? options.height : size;
    const color = options.color || 'currentColor';
    const className = options.className || options.class || '';
    const style = options.style || '';
    const viewBox = options.viewBox || '0 0 24 24';
    const title = options.title || '';
    const ariaLabel = options.ariaLabel || '';
    const role = options.role || (title || ariaLabel ? 'img' : 'graphics-symbol');
    const asElement = Boolean(options.asElement);
    const attributes = options.attributes || {};

    const formattedWidth = formatDimension(width);
    const formattedHeight = formatDimension(height);

    let styleAttr = '';
    if (typeof style === 'object' && style !== null) {
      styleAttr = Object.entries(style)
        .map(([k, v]) => `${k.replace(/([A-Z])/g, '-$1').toLowerCase()}:${v}`)
        .join(';');
    } else {
      styleAttr = String(style);
    }

    const ariaAttrs = (title || ariaLabel)
      ? `role="${escapeAttr(role)}" ${ariaLabel ? `aria-label="${escapeAttr(ariaLabel)}"` : ''}`
      : 'aria-hidden="true"';

    const titleEl = title ? `<title>${escapeXml(title)}</title>` : '';

    const customAttrs = Object.entries(attributes)
      .map(([k, v]) => `${escapeAttr(k)}="${escapeAttr(v)}"`)
      .join(' ');

    const classAttr = className ? `class="${escapeAttr(className)}"` : '';
    const styleString = styleAttr ? `style="${escapeAttr(styleAttr)}"` : '';

    const viewBoxParts = viewBox.split(/\s+/).map(Number);
    const vbWidth = !isNaN(viewBoxParts[2]) ? viewBoxParts[2] : 24;
    const vbHeight = !isNaN(viewBoxParts[3]) ? viewBoxParts[3] : 24;

    const svgMarkup = [
      `<svg xmlns="http://www.w3.org/2000/svg"`,
      `width="${formattedWidth}"`,
      `height="${formattedHeight}"`,
      `viewBox="${escapeAttr(viewBox)}"`,
      classAttr,
      styleString,
      ariaAttrs,
      customAttrs,
      `>`,
      `<path d="M0 0h${vbWidth}v${vbHeight}H0z" fill="none"/>`,
      titleEl,
      pathBuilder(color, options),
      `</svg>`
    ].filter(Boolean).join(' ').replace(/\s+/g, ' ');

    if (asElement && typeof document !== 'undefined') {
      const template = document.createElement('div');
      template.innerHTML = svgMarkup.trim();
      return template.firstElementChild;
    }

    return svgMarkup;
  }

  const IconSets = {
    /**
     * Daze Square Solid Icon
     * @param {Object} [options] - Configuration options for size, color, fill, className, asElement, etc.
     * @returns {string|SVGElement}
     */
    dazeSquareSolid(options = {}) {
      return buildSvg(
        (color, opts) => `<path fill="${escapeAttr(opts.fill || color)}" d="M9.367 2.25c-1.092 0-1.958 0-2.655.057c-.714.058-1.317.18-1.868.46a4.75 4.75 0 0 0-2.076 2.077c-.281.55-.403 1.154-.461 1.868c-.057.697-.057 1.563-.057 2.655v5.266c0 1.092 0 1.958.057 2.655c.058.714.18 1.317.46 1.869a4.75 4.75 0 0 0 2.077 2.075c.55.281 1.154.403 1.868.461c.697.057 1.563.057 2.655.057h5.266c1.092 0 1.958 0 2.655-.057c.714-.058 1.317-.18 1.869-.46a4.75 4.75 0 0 0 2.075-2.076c.281-.552.403-1.155.461-1.869c.057-.697.057-1.563.057-2.655V9.367c0-1.092 0-1.958-.057-2.655c-.058-.714-.18-1.317-.46-1.868a4.75 4.75 0 0 0-2.076-2.076c-.552-.281-1.155-.403-1.869-.461c-.697-.057-1.563-.057-2.655-.057zm-.484 12.4a.75.75 0 0 1 .9 0l.884.662l.883-.662a.75.75 0 0 1 .9 0l.883.662l.884-.662a.75.75 0 0 1 .9 0l1.333 1a.75.75 0 1 1-.9 1.2l-.883-.663l-.884.663a.75.75 0 0 1-.9 0L12 16.187l-.883.663a.75.75 0 0 1-.9 0l-.884-.663l-.883.663a.75.75 0 1 1-.9-1.2zM8.45 8.4l2 1.5a.75.75 0 0 1 0 1.2l-2 1.5a.75.75 0 1 1-.9-1.2l1.2-.9l-1.2-.9a.75.75 0 0 1 .9-1.2m8.15.15a.75.75 0 0 1-.15 1.05l-1.2.9l1.2.9a.75.75 0 1 1-.9 1.2l-2-1.5a.75.75 0 0 1 0-1.2l2-1.5a.75.75 0 0 1 1.05.15"/>`,
        options
      );
    },

    /**
     * Daze Square Stroked Icon
     * @param {Object} [options] - Configuration options for size, color, stroke, strokeWidth, strokeLinecap, strokeLinejoin, asElement, etc.
     * @returns {string|SVGElement}
     */
    dazeSquare(options = {}) {
      return buildSvg(
        (color, opts) => {
          const stroke = escapeAttr(opts.stroke || color);
          const strokeWidth = opts.strokeWidth !== undefined ? opts.strokeWidth : 1.5;
          const strokeLinecap = escapeAttr(opts.strokeLinecap || 'round');
          const strokeLinejoin = escapeAttr(opts.strokeLinejoin || 'round');
          return `<g fill="none" stroke="${stroke}" stroke-linecap="${strokeLinecap}" stroke-linejoin="${strokeLinejoin}" stroke-width="${strokeWidth}">
            <path d="m8 12l2-1.5L8 9m8 3l-2-1.5L16 9m0 7.25l-1.333-1l-1.334 1l-1.333-1l-1.333 1l-1.334-1l-1.333 1"/>
            <path d="M3 9.4c0-2.24 0-3.36.436-4.216a4 4 0 0 1 1.748-1.748C6.04 3 7.16 3 9.4 3h5.2c2.24 0 3.36 0 4.216.436a4 4 0 0 1 1.748 1.748C21 6.04 21 7.16 21 9.4v5.2c0 2.24 0 3.36-.436 4.216a4 4 0 0 1-1.748 1.748C17.96 21 16.84 21 14.6 21H9.4c-2.24 0-3.36 0-4.216-.436a4 4 0 0 1-1.748-1.748C3 17.96 3 16.84 3 14.6z"/>
          </g>`;
        },
        options
      );
    },

    /**
     * Code Square Solid Icon
     * @param {Object} [options] - Configuration options for size, color, fill, className, asElement, etc.
     * @returns {string|SVGElement}
     */
    codeSquareSolid(options = {}) {
      return buildSvg(
        (color, opts) => `<path fill="${escapeAttr(opts.fill || color)}" d="M9.367 2.25h5.266c1.092 0 1.958 0 2.655.057c.714.058 1.317.18 1.869.46a4.75 4.75 0 0 1 2.075 2.077c.281.55.403 1.154.461 1.868c.057.697.057 1.563.057 2.655v5.266c0 1.092 0 1.958-.057 2.655c-.058.714-.18 1.317-.46 1.869a4.75 4.75 0 0 1-2.076 2.075c-.552.281-1.155.403-1.869.461c-.697.057-1.563.057-2.655.057H9.367c-1.092 0-1.958 0-2.655-.057c-.714-.058-1.317-.18-1.868-.46a4.75 4.75 0 0 1-2.076-2.076c-.281-.552-.403-1.155-.461-1.869c-.057-.697-.057-1.563-.057-2.655V9.367c0-1.092 0-1.958.057-2.655c.058-.714.18-1.317.46-1.868a4.75 4.75 0 0 1 2.077-2.076c.55-.281 1.154-.403 1.868-.461c.697-.057 1.563-.057 2.655-.057m4.43 5.944a.75.75 0 1 0-1.45-.388l-2.143 8a.75.75 0 0 0 1.449.388zm1.641.975a.75.75 0 1 0-1.06 1.06l.131.132c.527.526.867.869 1.085 1.155c.205.268.23.396.23.484s-.025.216-.23.484c-.218.286-.558.629-1.085 1.155l-.131.131a.75.75 0 1 0 1.06 1.06l.167-.166c.482-.48.895-.894 1.181-1.27c.307-.402.537-.846.537-1.394s-.23-.992-.537-1.394c-.286-.376-.7-.79-1.18-1.27zm-5.816 0a.75.75 0 0 0-1.06 0l-.167.167c-.481.48-.895.894-1.181 1.27c-.307.402-.537.846-.537 1.394s.23.992.537 1.394c.286.376.7.79 1.18 1.27l.168.167a.75.75 0 0 0 1.06-1.06l-.131-.132c-.527-.526-.867-.869-1.085-1.155c-.205-.268-.23-.396-.23-.484s.025-.216.23-.484c.218-.286.558-.629 1.085-1.155l.131-.131a.75.75 0 0 0 0-1.061"/>`,
        options
      );
    },

    /**
     * Code Square Stroked Icon
     * @param {Object} [options] - Configuration options for size, color, stroke, strokeWidth, strokeLinecap, strokeLinejoin, asElement, etc.
     * @returns {string|SVGElement}
     */
    codeSquare(options = {}) {
      return buildSvg(
        (color, opts) => {
          const stroke = escapeAttr(opts.stroke || color);
          const strokeWidth = opts.strokeWidth !== undefined ? opts.strokeWidth : 1.5;
          const strokeLinecap = escapeAttr(opts.strokeLinecap || 'round');
          const strokeLinejoin = escapeAttr(opts.strokeLinejoin || 'round');
          return `<g fill="none" stroke="${stroke}" stroke-linecap="${strokeLinecap}" stroke-linejoin="${strokeLinejoin}" stroke-width="${strokeWidth}">
            <path d="M3 9.4c0-2.24 0-3.36.436-4.216a4 4 0 0 1 1.748-1.748C6.04 3 7.16 3 9.4 3h5.2c2.24 0 3.36 0 4.216.436a4 4 0 0 1 1.748 1.748C21 6.04 21 7.16 21 9.4v5.2c0 2.24 0 3.36-.436 4.216a4 4 0 0 1-1.748 1.748C17.96 21 16.84 21 14.6 21H9.4c-2.24 0-3.36 0-4.216-.436a4 4 0 0 1-1.748-1.748C3 17.96 3 16.84 3 14.6z"/>
            <path d="m14.908 9.7l.132.131c1.022 1.022 1.534 1.534 1.534 2.169s-.512 1.146-1.534 2.169l-.132.132M13.072 8l-2.143 8M9.092 9.7l-.132.131C7.938 10.853 7.427 11.365 7.427 12s.51 1.146 1.533 2.169l.132.132"/>
          </g>`;
        },
        options
      );
    },

    /**
     * Triangle Pointing Right Icon
     * @param {Object} [options] - Configuration options for size, color, fill, className, asElement, etc.
     * @returns {string|SVGElement}
     */
    trianglePointingRight(options = {}) {
      return buildSvg(
        (color, opts) => `<path fill="${escapeAttr(opts.fill || color)}" d="M7 28a1 1 0 0 1-1-1V5a1 1 0 0 1 1.482-.876l20 11a1 1 0 0 1 0 1.752l-20 11A1 1 0 0 1 7 28M8 6.69v18.62L24.925 16Z"/>`,
        { viewBox: '0 0 32 32', ...options }
      );
    },


    /**
     * Triangle Pointing Right Solid Icon
     * @param {Object} [options] - Configuration options for size, color, fill, className, asElement, etc.
     * @returns {string|SVGElement}
     */
    trianglePointingRightSolid(options = {}) {
      return buildSvg(
        (color, opts) => `<path fill="${escapeAttr(opts.fill || color)}" d="M1 15.65L15.662 8L1 .35z"/>`,
        { viewBox: '0 0 16 16', ...options }
      );
    },

    /**
     * Render icon by key name.
     * @param {string} name - Icon key name, e.g., 'dazeSquareSolid', 'daze-square-solid', 'codeSquare', etc.
     * @param {Object} [options] - Configurable options.
     * @returns {string|SVGElement}
     */
    render(name, options = {}) {
      if (!name) return '';
      const camelName = name.replace(/-([a-z])/g, (_, letter) => letter.toUpperCase());
      if (typeof this[camelName] === 'function' && camelName !== 'render') {
        return this[camelName](options);
      }
      console.warn(`[IconSets] Unknown icon name: "${name}"`);
      return '';
    }
  };

  return IconSets;
}));