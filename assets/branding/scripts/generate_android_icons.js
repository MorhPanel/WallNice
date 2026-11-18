const fs = require('fs');
const path = require('path');

const resDir = path.join(__dirname, 'android', 'app', 'src', 'main', 'res');
const drawableDir = path.join(resDir, 'drawable');
const brandingResDir = path.join(__dirname, 'assets', 'branding', 'android_resources');

if (!fs.existsSync(brandingResDir)) {
  fs.mkdirSync(brandingResDir, { recursive: true });
}

// 1. ic_launcher_background.xml
const backgroundXml = `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Dark OLED App Background -->
    <path
        android:fillColor="#090A10"
        android:pathData="M0,0h108v108h-108z" />
    <!-- Ambient Neon Pink Glow Center -->
    <path
        android:fillColor="#15FF2A6D"
        android:pathData="M54,54m-42,0a42,42 0,1 1,84 0a42,42 0,1 1,-84 0" />
    <!-- Ambient Neon Cyan Glow -->
    <path
        android:fillColor="#1005D9E8"
        android:pathData="M54,54m-28,0a28,28 0,1 1,56 0a28,28 0,1 1,-56 0" />
</vector>
`;

// 2. ic_launcher_foreground.xml (Dynamic Prism W within safe zone)
const foregroundXml = `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    
    <!-- Left Outer Ribbon (Neon Cyan) -->
    <path
        android:pathData="M 25.3,34.8 L 36.9,29.5 L 46.4,69.6 L 34.8,74.9 Z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="25.3"
                android:startY="29.5"
                android:endX="46.4"
                android:endY="74.9"
                android:type="linear">
                <item android:offset="0.0" android:color="#5DF6FF" />
                <item android:offset="1.0" android:color="#05D9E8" />
            </gradient>
        </aapt:attr>
    </path>

    <!-- Left Inner Ribbon (Cyan to Violet) -->
    <path
        android:pathData="M 34.8,74.9 L 46.4,69.6 L 54.0,46.4 L 45.4,46.4 Z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="34.8"
                android:startY="74.9"
                android:endX="54.0"
                android:endY="46.4"
                android:type="linear">
                <item android:offset="0.0" android:color="#0066FF" />
                <item android:offset="1.0" android:color="#7928CA" />
            </gradient>
        </aapt:attr>
    </path>

    <!-- Right Inner Ribbon (Violet to Pink) -->
    <path
        android:pathData="M 54.0,46.4 L 62.6,46.4 L 73.2,74.9 L 61.6,69.6 Z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="54.0"
                android:startY="46.4"
                android:endX="73.2"
                android:endY="74.9"
                android:type="linear">
                <item android:offset="0.0" android:color="#7928CA" />
                <item android:offset="1.0" android:color="#FF2A6D" />
            </gradient>
        </aapt:attr>
    </path>

    <!-- Right Outer Ribbon (Neon Pink) -->
    <path
        android:pathData="M 61.6,69.6 L 73.2,74.9 L 82.7,29.5 L 71.1,34.8 Z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="61.6"
                android:startY="69.6"
                android:endX="82.7"
                android:endY="29.5"
                android:type="linear">
                <item android:offset="0.0" android:color="#FF2A6D" />
                <item android:offset="1.0" android:color="#FF5E97" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
`;

// 3. ic_launcher_monochrome.xml (Android 13+ Material You themed icons)
const monochromeXml = `<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M 25.3,34.8 L 36.9,29.5 L 46.4,69.6 L 34.8,74.9 Z" />
    <path
        android:fillColor="#CCFFFFFF"
        android:pathData="M 34.8,74.9 L 46.4,69.6 L 54.0,46.4 L 45.4,46.4 Z" />
    <path
        android:fillColor="#E0FFFFFF"
        android:pathData="M 54.0,46.4 L 62.6,46.4 L 73.2,74.9 L 61.6,69.6 Z" />
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M 61.6,69.6 L 73.2,74.9 L 82.7,29.5 L 71.1,34.8 Z" />
</vector>
`;

// 4. ic_launcher.xml (Adaptive icon definition)
const adaptiveIconXml = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
`;

// Write to drawable and to branding assets
fs.writeFileSync(path.join(drawableDir, 'ic_launcher_background.xml'), backgroundXml, 'utf8');
fs.writeFileSync(path.join(drawableDir, 'ic_launcher_foreground.xml'), foregroundXml, 'utf8');
fs.writeFileSync(path.join(drawableDir, 'ic_launcher_monochrome.xml'), monochromeXml, 'utf8');
fs.writeFileSync(path.join(drawableDir, 'ic_launcher_wallnice.xml'), adaptiveIconXml, 'utf8');

fs.writeFileSync(path.join(brandingResDir, 'ic_launcher_background.xml'), backgroundXml, 'utf8');
fs.writeFileSync(path.join(brandingResDir, 'ic_launcher_foreground.xml'), foregroundXml, 'utf8');
fs.writeFileSync(path.join(brandingResDir, 'ic_launcher_monochrome.xml'), monochromeXml, 'utf8');
fs.writeFileSync(path.join(brandingResDir, 'ic_launcher_wallnice.xml'), adaptiveIconXml, 'utf8');

console.log('Android adaptive icon vector drawables created successfully!');
