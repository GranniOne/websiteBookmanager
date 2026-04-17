import EmbedPDF from './embedpdf.js';

export async function init(element, source) {
    const brandColors = [
        {
            name: 'Purple',
            primary: '#9333ea',
            hover: '#7e22ce',
            active: '#6b21a8',
            light: '#f3e8ff',
            darkLight: '#3b0764'
        },
        {name: 'Blue', primary: '#2563eb', hover: '#1d4ed8', active: '#1e40af', light: '#dbeafe', darkLight: '#1e3a8a'},
        {
            name: 'Green',
            primary: '#16a34a',
            hover: '#15803d',
            active: '#166534',
            light: '#dcfce7',
            darkLight: '#14532d'
        },
        {
            name: 'Orange',
            primary: '#ea580c',
            hover: '#c2410c',
            active: '#9a3412',
            light: '#ffedd5',
            darkLight: '#7c2d12'
        },
        {name: 'Pink', primary: '#db2777', hover: '#be185d', active: '#9d174d', light: '#fce7f3', darkLight: '#831843'},
    ];

    const themeModes = ['light', 'dark', 'system'];
    let selectedColor = brandColors[0];
    let selectedMode = 'system';

    const selectedColorName = document.getElementById('selected-color-name');
    const selectedThemeMode = document.getElementById('selected-theme-mode');
    const colorButtons = document.getElementById('color-buttons');
    const themeButtons = document.getElementById('theme-buttons');
    const viewerElement = document.getElementById('pdf-viewer');

    const toolBar = document.getElementById('toolbar-id');
    Object.assign(toolBar.style, {
        display: "flex",
        flexDirection: "column",
        gap: "12px",
        padding: "16px",
        backgroundColor: "#ffffff",
        border: "1px solid #e2e8f0",
        borderRadius: "12px",
        boxShadow: "0 1px 2px rgba(0, 0, 0, 0.05)",
        alignSelf: "center",
        width: "fit-content",
        marginTop: "12px",
        marginBottom: "12px",
    });

    // Ensure children (the rows) are flex containers
    const rows = toolBar.querySelectorAll('div');
    rows.forEach(row => {
        row.style.display = "flex";
        row.style.alignItems = "center";
        row.style.gap = "12px";
    });

    // Setup containers for flex spacing
    [colorButtons, themeButtons].forEach(el => {
        if (!el) return;
        el.style.display = "flex";
        el.style.gap = "12px";
        el.style.alignItems = "center";
        el.style.padding = "8px 0";
    });

    if (!viewerElement) return; // 💥 prevents crash if Vaadin not ready

    function buildTheme(color, mode) {
        return {
            preference: mode,
            light: {
                accent: {
                    primary: color.primary,
                    primaryHover: color.hover,
                    primaryActive: color.active,
                    primaryLight: color.light,
                    primaryForeground: '#ffffff',
                },
            },
            dark: {
                accent: {
                    primary: color.primary,
                    primaryHover: color.hover,
                    primaryActive: color.active,
                    primaryLight: color.darkLight,
                    primaryForeground: '#ffffff',
                },
            },
        };
    }

    const viewer = EmbedPDF.init({
        type: 'container',
        target: viewerElement,
        src: source,
        theme: buildTheme(selectedColor, selectedMode),
    });
    const registry = await viewer.registry;
    const commands = registry.getPlugin('commands')?.provides();
    const ui = registry.getPlugin('ui')?.provides();


    if (!commands || !ui) {
        statusDot.className = 'inline-block h-2 w-2 rounded-full bg-red-500';
        statusText.textContent = 'Unable to load UI plugins.';
        throw new Error('Commands or UI plugin not available');
    }


    viewer.registerIcons({
        customSmiley: {
            viewBox: '0 0 24 24',
            paths: [
                {
                    d: 'M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0',
                    stroke: 'currentColor',
                    fill: 'none',
                },
                {d: 'M9 10l.01 0', stroke: 'currentColor', fill: 'none'},
                {d: 'M15 10l.01 0', stroke: 'currentColor', fill: 'none'},
                {
                    d: 'M9.5 15a3.5 3.5 0 0 0 5 0',
                    stroke: 'currentColor',
                    fill: 'none',
                },
            ],
        },
        customStar: {
            viewBox: '0 0 24 24',
            paths: [
                {
                    d: 'M12 17.75l-6.172 3.245l1.179 -6.873l-5 -4.867l6.9 -1l3.086 -6.253l3.086 6.253l6.9 1l-5 4.867l1.179 6.873z',
                    stroke: 'currentColor',
                    fill: 'none',
                },
            ],
        },
    });


    commands.registerCommand({
        id: 'custom.smiley',
        label: 'Say Hello',
        icon: 'customSmiley',
        action: () => showAction(),
    });

    function showAction() {
        // Check the current style
        const isHidden = toolBar.style.display === 'none';

        // If it's 'none', set it back to 'inline-flex' (your "white box" style)
        // If it's visible, hide it
        toolBar.style.display = isHidden ? 'inline-flex' : 'none';

        console.log(`Toolbar is now ${isHidden ? 'visible' : 'hidden'}`);
    }


    commands.registerCommand({
        id: 'custom.star',
        label: 'Add to Favorites',
        icon: 'customStar',
        action: () => showAction('Added to favorites! ⭐'),
    });


    const currentSchema = ui.getSchema();
    const mainToolbar = currentSchema.toolbars['main-toolbar'];


    if (mainToolbar) {
        const items = structuredClone(mainToolbar.items);
        const rightGroup = items.find((item) => item.id === 'right-group');


        if (rightGroup && Array.isArray(rightGroup.items)) {
            const commentIndex = rightGroup.items.findIndex(
                (item) => item.id === 'comment-button',
            );


            if (commentIndex !== -1) {
                // Use splice to insert at commentIndex WITHOUT deleting (0) the original
                rightGroup.items.splice(commentIndex + 1, 0, {
                    type: 'command-button',
                    id: 'smiley-button',
                    commandId: 'custom.smiley',
                    variant: 'icon',
                });
            }
        }


        ui.mergeSchema({
            toolbars: {
                'main-toolbar': {
                    ...mainToolbar,
                    items,
                },
            },
        });
    }


    const documentMenu = currentSchema.menus['document-menu'];


    if (documentMenu) {
        ui.mergeSchema({
            menus: {
                'document-menu': {
                    ...documentMenu,
                    items: [
                        ...documentMenu.items,
                        {type: 'divider', id: 'custom-divider'},
                        {type: 'command', id: 'star-menu-item', commandId: 'custom.star'},
                    ],
                },
            },
        });
    }

    function applyTheme() {
        viewer.setTheme(buildTheme(selectedColor, selectedMode));

        // Format: Selected: **Color**
        selectedColorName.innerHTML = `Selected: <strong style="color: #0f172a; font-weight: 600;">${selectedColor.name}</strong>`;
        selectedColorName.style.color = "#64748b";

        // Format: Active mode: **System**
        selectedThemeMode.innerHTML = `Active mode: <strong style="color: #0f172a; font-weight: 600;">${selectedMode}</strong>`;
        selectedThemeMode.style.color = "#64748b";

        renderButtons();
    }

    // Use the circular button logic with the white ring from before
    function createColorButton(color) {
        const button = document.createElement('button');
        const isSelected = selectedColor.name === color.name;

        Object.assign(button.style, {
            width: "28px",
            height: "28px",
            borderRadius: "50%",
            backgroundColor: color.primary,
            border: "none",
            cursor: "pointer",
            transition: "all 0.2s cubic-bezier(0.4, 0, 0.2, 1)",
            outline: isSelected ? "2px solid #ffffff" : "2px solid transparent",
            outlineOffset: "2px",
            boxShadow: isSelected ? `0 0 0 4px ${color.primary}44` : "none",
            transform: isSelected ? "scale(1.15)" : "scale(1)"
        });

        button.onclick = () => {
            selectedColor = color;
            applyTheme();
        };
        return button;
    }

    function createThemeButton(mode) {
        const button = document.createElement('button');
        const isSelected = selectedMode === mode;

        // Pill Styling to match the modern "white box" look
        Object.assign(button.style, {
            padding: "6px 16px",
            borderRadius: "20px",
            cursor: "pointer",
            fontSize: "13px",
            fontWeight: "500",
            transition: "all 0.2s ease",
            // No border when selected, subtle slate border when inactive
            border: isSelected ? "none" : "1px solid #e2e8f0",
            // Black background for active, light gray/white for inactive
            backgroundColor: isSelected ? "#000000" : "#f8fafc",
            // White text for active, muted slate for inactive
            color: isSelected ? "#ffffff" : "#64748b",
            outline: "none"
        });

        button.textContent = mode.charAt(0).toUpperCase() + mode.slice(1);

        button.onclick = () => {
            selectedMode = mode;
            applyTheme();
        };

        // Optional: Add a subtle hover effect via JS since we're staying in-script
        button.onmouseenter = () => {
            if (!isSelected) button.style.backgroundColor = "#f1f5f9";
        };
        button.onmouseleave = () => {
            if (!isSelected) button.style.backgroundColor = "#f8fafc";
        };

        return button;
    }

    function renderButtons() {
        if (colorButtons) colorButtons.replaceChildren(...brandColors.map(createColorButton));
        if (themeButtons) themeButtons.replaceChildren(...themeModes.map(createThemeButton));
    }

    applyTheme();
}

// 👇 CRITICAL for Vaadin
window.initEmbedPdf = init;




/*
export function initEmbedPdf(container, pdfSrc) {
    (async () => {
        const EmbedPDF = (await import('./embedpdf.js')).default;

        EmbedPDF.init({
            type: 'container',
            target: container,
            src: pdfSrc,
            theme: {
                preference: 'system',
                light: {
                    accent: {
                        primary: '#9333ea',        // Main brand color
                        primaryHover: '#7e22ce',   // Hover state
                        primaryActive: '#6b21a8',  // Click state
                        primaryLight: '#f3e8ff',   // Subtle backgrounds (e.g., selection)
                        primaryForeground: '#fff'  // Text on top of primary color
                    }
                },
                dark: {
                    accent: {
                        primary: '#a855f7',        // Lighter purple for dark mode
                        primaryHover: '#9333ea',
                        // ... other states
                    }
                }
            }
        });
    })();
}

 */