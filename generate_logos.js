const fs = require('fs');
const path = require('path');

const outDir = path.join(__dirname, 'assets', 'branding');
if (!fs.existsSync(outDir)) {
  fs.mkdirSync(outDir, { recursive: true });
}

// Color Palette
const COLORS = {
  bgDark: '#090A10',
  bgCard: '#141522',
  bgSurface: '#10111A',
  neonPink: '#FF2A6D',
  neonPinkLight: '#FF5E97',
  neonCyan: '#05D9E8',
  neonCyanLight: '#5DF6FF',
  neonPurple: '#8A2BE2',
  neonViolet: '#7928CA',
  white: '#F1F1F6',
  muted: '#9092A5',
  monochromeWhite: '#FFFFFF',
  monochromeDark: '#12131A'
};

// ==========================================
// 1. CONCEPT 1: DYNAMIC PRISM W
// ==========================================
function getConcept1Svg(variant = 'dark') {
  const isDark = variant === 'dark';
  const isMono = variant === 'monochrome';
  
  const bg = isDark ? `
    <rect width="512" height="512" rx="112" fill="${COLORS.bgDark}"/>
    <rect width="510" height="510" x="1" y="1" rx="111" fill="none" stroke="rgba(255, 42, 109, 0.15)" stroke-width="2"/>
    <circle cx="256" cy="256" r="180" fill="url(#c1-ambient-glow)" opacity="0.35"/>
  ` : '';

  const gradLeft = isMono 
    ? 'fill="#FFFFFF"' 
    : 'fill="url(#c1-grad-left)"';
  const gradLeftInner = isMono 
    ? 'fill="#CCCCCC"' 
    : 'fill="url(#c1-grad-left-inner)"';
  const gradRightInner = isMono 
    ? 'fill="#E0E0E0"' 
    : 'fill="url(#c1-grad-right-inner)"';
  const gradRight = isMono 
    ? 'fill="#FFFFFF"' 
    : 'fill="url(#c1-grad-right)"';

  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="100%" height="100%">
  <defs>
    <radialGradient id="c1-ambient-glow" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="${COLORS.neonCyan}" stop-opacity="0.3"/>
      <stop offset="50%" stop-color="${COLORS.neonPink}" stop-opacity="0.15"/>
      <stop offset="100%" stop-color="${COLORS.bgDark}" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="c1-grad-left" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="${COLORS.neonCyanLight}"/>
      <stop offset="100%" stop-color="#0077FE"/>
    </linearGradient>
    <linearGradient id="c1-grad-left-inner" x1="0%" y1="100%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="#0066FF"/>
      <stop offset="100%" stop-color="${COLORS.neonViolet}"/>
    </linearGradient>
    <linearGradient id="c1-grad-right-inner" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="${COLORS.neonViolet}"/>
      <stop offset="100%" stop-color="${COLORS.neonPink}"/>
    </linearGradient>
    <linearGradient id="c1-grad-right" x1="0%" y1="100%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="${COLORS.neonPink}"/>
      <stop offset="100%" stop-color="${COLORS.neonPinkLight}"/>
    </linearGradient>
    <filter id="c1-glow" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="12" result="blur"/>
      <feComposite in="SourceGraphic" in2="blur" operator="over"/>
    </filter>
  </defs>
  ${bg}
  <g ${!isMono ? 'filter="url(#c1-glow)"' : ''} id="icon-glyph">
    <!-- Left outer ribbon -->
    <path d="M 120 165 L 175 140 L 220 330 L 165 355 Z" ${gradLeft} />
    <!-- Left inner ribbon -->
    <path d="M 165 355 L 220 330 L 256 220 L 215 220 Z" ${gradLeftInner} />
    <!-- Right inner ribbon -->
    <path d="M 256 220 L 297 220 L 347 355 L 292 330 Z" ${gradRightInner} />
    <!-- Right outer ribbon -->
    <path d="M 292 330 L 347 355 L 392 140 L 337 165 Z" ${gradRight} />
    <!-- Dynamic Fold Shadow & Highlight -->
    ${!isMono ? `
    <polygon points="220,330 256,220 236,250 195,340" fill="#000000" opacity="0.25"/>
    <polygon points="292,330 256,220 276,250 317,340" fill="#FFFFFF" opacity="0.15"/>
    ` : ''}
  </g>
</svg>`;
}

// ==========================================
// 2. CONCEPT 2: SCREEN CASCADE / LIVE LAYERS
// ==========================================
function getConcept2Svg(variant = 'dark') {
  const isDark = variant === 'dark';
  const isMono = variant === 'monochrome';

  const bg = isDark ? `
    <rect width="512" height="512" rx="112" fill="${COLORS.bgDark}"/>
    <rect width="510" height="510" x="1" y="1" rx="111" fill="none" stroke="rgba(5, 217, 232, 0.15)" stroke-width="2"/>
    <circle cx="256" cy="256" r="170" fill="url(#c2-ambient)" opacity="0.3"/>
  ` : '';

  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="100%" height="100%">
  <defs>
    <radialGradient id="c2-ambient" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="${COLORS.neonPink}" stop-opacity="0.25"/>
      <stop offset="60%" stop-color="${COLORS.neonCyan}" stop-opacity="0.1"/>
      <stop offset="100%" stop-color="${COLORS.bgDark}" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="c2-card1" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="${COLORS.neonCyan}"/>
      <stop offset="100%" stop-color="#0066FF"/>
    </linearGradient>
    <linearGradient id="c2-card2" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#7928CA"/>
      <stop offset="100%" stop-color="${COLORS.neonPink}"/>
    </linearGradient>
    <linearGradient id="c2-card3" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="${COLORS.neonPinkLight}"/>
      <stop offset="100%" stop-color="${COLORS.neonPink}"/>
    </linearGradient>
    <filter id="c2-glow" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="8" result="blur"/>
      <feComposite in="SourceGraphic" in2="blur" operator="over"/>
    </filter>
  </defs>
  ${bg}
  <g ${!isMono ? 'filter="url(#c2-glow)"' : ''} id="icon-glyph">
    <!-- Back Screen Layer (Static) -->
    <rect x="136" y="140" width="130" height="210" rx="18" 
          fill="${isMono ? '#666666' : '#141522'}" 
          stroke="${isMono ? '#999999' : 'url(#c2-card1)'}" 
          stroke-width="8" 
          transform="rotate(-14 201 245)"/>
    
    <!-- Mid Screen Layer (Animated / GIF) -->
    <rect x="246" y="140" width="130" height="210" rx="18" 
          fill="${isMono ? '#999999' : '#1A1829'}" 
          stroke="${isMono ? '#CCCCCC' : 'url(#c2-card2)'}" 
          stroke-width="8" 
          transform="rotate(14 311 245)"/>

    <!-- Front Center Layer (Live / Interactive Wallpaper) forming dynamic W center -->
    <rect x="191" y="156" width="130" height="210" rx="18" 
          fill="${isMono ? '#FFFFFF' : '#10111A'}" 
          stroke="${isMono ? '#FFFFFF' : 'url(#c2-card3)'}" 
          stroke-width="9"/>

    <!-- Inside the Front Layer: Live Horizon Wave & Sun/Portal -->
    ${!isMono ? `
    <!-- Glowing wallpaper gradient fill inside front card -->
    <path d="M 196 270 Q 225 240 256 260 T 316 250 L 316 358 A 12 12 0 0 1 308 362 L 204 362 A 12 12 0 0 1 196 352 Z" 
          fill="url(#c2-card2)" opacity="0.6"/>
    <circle cx="256" cy="225" r="22" fill="url(#c2-card1)" opacity="0.9"/>
    <!-- Top speaker bar/pill -->
    <rect x="241" y="168" width="30" height="4" rx="2" fill="${COLORS.muted}" opacity="0.6"/>
    ` : `
    <circle cx="256" cy="225" r="22" fill="#12131A"/>
    <path d="M 200 280 Q 230 250 256 270 T 312 260 L 312 360 L 200 360 Z" fill="#12131A"/>
    `}
  </g>
</svg>`;
}

// ==========================================
// 3. CONCEPT 3: AURA PORTAL / SMARTPHONE FRAME & W
// ==========================================
function getConcept3Svg(variant = 'dark') {
  const isDark = variant === 'dark';
  const isMono = variant === 'monochrome';

  const bg = isDark ? `
    <rect width="512" height="512" rx="112" fill="${COLORS.bgDark}"/>
    <rect width="510" height="510" x="1" y="1" rx="111" fill="none" stroke="rgba(255, 255, 255, 0.08)" stroke-width="2"/>
    <circle cx="256" cy="256" r="175" fill="url(#c3-radial)" opacity="0.4"/>
  ` : '';

  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="100%" height="100%">
  <defs>
    <radialGradient id="c3-radial" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="${COLORS.neonPink}" stop-opacity="0.3"/>
      <stop offset="70%" stop-color="${COLORS.neonCyan}" stop-opacity="0.1"/>
      <stop offset="100%" stop-color="${COLORS.bgDark}" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="c3-cyan-grad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="${COLORS.neonCyanLight}"/>
      <stop offset="100%" stop-color="${COLORS.neonCyan}"/>
    </linearGradient>
    <linearGradient id="c3-pink-grad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="${COLORS.neonPinkLight}"/>
      <stop offset="100%" stop-color="${COLORS.neonPink}"/>
    </linearGradient>
    <linearGradient id="c3-w-grad" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="${COLORS.neonCyan}"/>
      <stop offset="50%" stop-color="#9933FF"/>
      <stop offset="100%" stop-color="${COLORS.neonPink}"/>
    </linearGradient>
    <filter id="c3-glow" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="10" result="blur"/>
      <feComposite in="SourceGraphic" in2="blur" operator="over"/>
    </filter>
  </defs>
  ${bg}
  <g ${!isMono ? 'filter="url(#c3-glow)"' : ''} id="icon-glyph">
    <!-- Top-Left Phone Frame Bracket (Cyan) -->
    <path d="M 230 130 L 160 130 C 138 130 120 148 120 170 L 120 280" 
          fill="none" 
          stroke="${isMono ? '#CCCCCC' : 'url(#c3-cyan-grad)'}" 
          stroke-width="14" 
          stroke-linecap="round"/>
    
    <!-- Bottom-Right Phone Frame Bracket (Pink) -->
    <path d="M 282 382 L 352 382 C 374 382 392 364 392 342 L 392 232" 
          fill="none" 
          stroke="${isMono ? '#CCCCCC' : 'url(#c3-pink-grad)'}" 
          stroke-width="14" 
          stroke-linecap="round"/>

    <!-- Central Floating Wallpaper Monogram "W" (Modern Faceted Polygon Shape) -->
    <path d="M 160 195 
             L 205 320 
             L 256 220 
             L 307 320 
             L 352 195 
             L 318 195 
             L 288 280 
             L 256 205 
             L 224 280 
             L 194 195 Z" 
          fill="${isMono ? '#FFFFFF' : 'url(#c3-w-grad)'}"/>

    <!-- Subtle Live Wallpaper Spark/Star in top-right corner of screen -->
    ${!isMono ? `
    <polygon points="340,150 344,162 356,166 344,170 340,182 336,170 324,166 336,162" 
             fill="${COLORS.neonCyanLight}"/>
    <circle cx="170" cy="350" r="4" fill="${COLORS.neonPink}"/>
    ` : ''}
  </g>
</svg>`;
}

// ==========================================
// 4. CONCEPT 4: NEON MOTION WAVE W (MINIMALIST LIVE PULSE)
// ==========================================
function getConcept4Svg(variant = 'dark') {
  const isDark = variant === 'dark';
  const isMono = variant === 'monochrome';

  const bg = isDark ? `
    <rect width="512" height="512" rx="112" fill="${COLORS.bgDark}"/>
    <rect width="510" height="510" x="1" y="1" rx="111" fill="none" stroke="rgba(255, 42, 109, 0.15)" stroke-width="2"/>
    <circle cx="256" cy="256" r="175" fill="url(#c4-pulse-ambient)" opacity="0.35"/>
  ` : '';

  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="100%" height="100%">
  <defs>
    <radialGradient id="c4-pulse-ambient" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="${COLORS.neonPink}" stop-opacity="0.3"/>
      <stop offset="60%" stop-color="${COLORS.neonCyan}" stop-opacity="0.12"/>
      <stop offset="100%" stop-color="${COLORS.bgDark}" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="c4-motion-wave" x1="0%" y1="30%" x2="100%" y2="70%">
      <stop offset="0%" stop-color="${COLORS.neonCyanLight}"/>
      <stop offset="25%" stop-color="${COLORS.neonCyan}"/>
      <stop offset="65%" stop-color="${COLORS.neonViolet}"/>
      <stop offset="100%" stop-color="${COLORS.neonPink}"/>
    </linearGradient>
    <linearGradient id="c4-inner-line" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="${COLORS.neonCyan}"/>
      <stop offset="100%" stop-color="${COLORS.neonPink}"/>
    </linearGradient>
    <filter id="c4-glow" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="12" result="blur"/>
      <feComposite in="SourceGraphic" in2="blur" operator="over"/>
    </filter>
  </defs>
  ${bg}
  <g ${!isMono ? 'filter="url(#c4-glow)"' : ''} id="icon-glyph">
    <!-- Screen Squircle Frame Outline (Abstract device display boundary) -->
    <rect x="106" y="106" width="300" height="300" rx="64" 
          fill="none" 
          stroke="${isMono ? '#444444' : 'rgba(255, 255, 255, 0.08)'}" 
          stroke-width="5"/>
    
    <!-- Primary Kinetic Motion "W" Wave (Bold, Ultra-Readable at 48x48) -->
    <path d="M 148 190 
             C 165 240, 185 320, 206 332
             C 220 340, 235 315, 256 245
             C 277 315, 292 340, 306 332
             C 327 320, 347 240, 364 190" 
          fill="none" 
          stroke="${isMono ? '#FFFFFF' : 'url(#c4-motion-wave)'}" 
          stroke-width="32" 
          stroke-linecap="round" 
          stroke-linejoin="round"/>

    <!-- Subtle Live Pulse Wave (Echo line indicating GIF / Live dynamics) -->
    <path d="M 180 185 
             C 192 220, 204 270, 214 278
             C 224 286, 238 260, 256 205
             C 274 260, 288 286, 298 278
             C 308 270, 320 220, 332 185" 
          fill="none" 
          stroke="${isMono ? '#888888' : 'url(#c4-inner-line)'}" 
          stroke-width="8" 
          stroke-linecap="round" 
          opacity="${isMono ? '0.5' : '0.65'}"/>

    <!-- Glowing Apex Sparkles (Live wallpaper interaction touchpoints) -->
    ${!isMono ? `
    <circle cx="256" cy="205" r="7" fill="${COLORS.neonCyanLight}"/>
    <circle cx="148" cy="190" r="5" fill="${COLORS.neonCyan}"/>
    <circle cx="364" cy="190" r="5" fill="${COLORS.neonPink}"/>
    ` : ''}
  </g>
</svg>`;
}

// ==========================================
// 5. WORDMARK COMBINATIONS (WALLNICE)
// ==========================================
function getWordmarkHorizontalSvg(conceptNum = 1, variant = 'dark') {
  const isDark = variant === 'dark';
  const isMono = variant === 'monochrome';
  
  const bg = isDark ? `<rect width="1024" height="320" rx="32" fill="${COLORS.bgDark}"/>` : '';

  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 320" width="100%" height="100%">
  <defs>
    <linearGradient id="wm-nice-grad" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" stop-color="${COLORS.neonCyan}"/>
      <stop offset="60%" stop-color="${COLORS.neonPink}"/>
      <stop offset="100%" stop-color="${COLORS.neonPinkLight}"/>
    </linearGradient>
    <filter id="wm-glow" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="8" result="blur"/>
      <feComposite in="SourceGraphic" in2="blur" operator="over"/>
    </filter>
  </defs>
  ${bg}
  <!-- Icon Symbol on the left -->
  <g transform="translate(60, 40) scale(0.46875)">
    <!-- Scale 512 down to 240 -->
    ${conceptNum === 1 ? getConcept1Svg('transparent').replace(/<svg[^>]*>|<\/svg>/g, '') : ''}
    ${conceptNum === 4 ? getConcept4Svg('transparent').replace(/<svg[^>]*>|<\/svg>/g, '') : ''}
  </g>

  <!-- Divider line -->
  <line x1="330" y1="80" x2="330" y2="240" stroke="rgba(255, 255, 255, 0.12)" stroke-width="2" />

  <!-- Typography: WallNice -->
  <g transform="translate(370, 185)">
    <text font-family="system-ui, -apple-system, 'SF Pro Display', 'Segoe UI', Roboto, sans-serif" 
          font-weight="900" 
          font-size="92" 
          letter-spacing="-1">
      <tspan fill="${isMono ? '#FFFFFF' : COLORS.white}">Wall</tspan><tspan fill="${isMono ? '#CCCCCC' : 'url(#wm-nice-grad)'}">Nice</tspan>
    </text>

    <!-- Tagline / Descriptor -->
    <text y="42" 
          font-family="system-ui, -apple-system, 'SF Pro Text', 'Segoe UI', Roboto, sans-serif" 
          font-weight="600" 
          font-size="16" 
          letter-spacing="6" 
          fill="${COLORS.muted}">
      4K • LIVE • GIF • WALLPAPERS
    </text>

    <!-- Glowing Accent Dot over "i" -->
    ${!isMono ? `<circle cx="282" cy="-68" r="7" fill="${COLORS.neonPink}" filter="url(#wm-glow)"/>` : ''}
  </g>
</svg>`;
}

// Generate all files
const filesToGenerate = [
  // Concept 1
  { name: 'concept1_prism_w_dark.svg', content: getConcept1Svg('dark') },
  { name: 'concept1_prism_w_transparent.svg', content: getConcept1Svg('transparent') },
  { name: 'concept1_prism_w_monochrome.svg', content: getConcept1Svg('monochrome') },

  // Concept 2
  { name: 'concept2_layers_dark.svg', content: getConcept2Svg('dark') },
  { name: 'concept2_layers_transparent.svg', content: getConcept2Svg('transparent') },
  { name: 'concept2_layers_monochrome.svg', content: getConcept2Svg('monochrome') },

  // Concept 3
  { name: 'concept3_portal_dark.svg', content: getConcept3Svg('dark') },
  { name: 'concept3_portal_transparent.svg', content: getConcept3Svg('transparent') },
  { name: 'concept3_portal_monochrome.svg', content: getConcept3Svg('monochrome') },

  // Concept 4
  { name: 'concept4_pulse_wave_dark.svg', content: getConcept4Svg('dark') },
  { name: 'concept4_pulse_wave_transparent.svg', content: getConcept4Svg('transparent') },
  { name: 'concept4_pulse_wave_monochrome.svg', content: getConcept4Svg('monochrome') },

  // Wordmark
  { name: 'wallnice_wordmark_c1_dark.svg', content: getWordmarkHorizontalSvg(1, 'dark') },
  { name: 'wallnice_wordmark_c1_transparent.svg', content: getWordmarkHorizontalSvg(1, 'transparent') },
  { name: 'wallnice_wordmark_c4_dark.svg', content: getWordmarkHorizontalSvg(4, 'dark') },
  { name: 'wallnice_wordmark_c4_transparent.svg', content: getWordmarkHorizontalSvg(4, 'transparent') }
];

filesToGenerate.forEach(f => {
  fs.writeFileSync(path.join(outDir, f.name), f.content, 'utf8');
  console.log(`Generated: ${f.name}`);
});
