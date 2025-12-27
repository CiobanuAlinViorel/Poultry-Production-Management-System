import { createContext, useContext, useEffect, useState } from "react";

type Theme = "dark" | "light";

type ThemeProviderProps = {
    children: React.ReactNode;
};

type ThemeProviderState = {
    theme: Theme;
    toggleTheme: () => void;
};

const ThemeProviderContext = createContext<ThemeProviderState | undefined>(
    undefined
);

export function ThemeProvider({ children }: ThemeProviderProps) {
    const [theme, setTheme] = useState<Theme>(() => {
        // Check localStorage first
        const stored = localStorage.getItem("theme") as Theme | null;
        if (stored) return stored;

        // Fallback to system preference
        return window.matchMedia("(prefers-color-scheme: dark)").matches
            ? "dark"
            : "light";
    });

    useEffect(() => {
        const root = document.documentElement;
        const body = document.body;

        // ✅ CRITICAL: Remove existing classes from both html and body
        root.classList.remove("dark", "light");
        body.classList.remove("dark", "light");

        // ✅ Add theme class to BOTH html AND body (for Tailwind v4)
        if (theme === "dark") {
            root.classList.add("dark");
            body.classList.add("dark");
        } else {
            root.classList.add("light");
            body.classList.add("light");
        }

        // ✅ Save to localStorage
        localStorage.setItem("theme", theme);

        // 🐛 Debug logging
        console.log("=== THEME DEBUG ===");
        console.log("Theme:", theme);
        console.log("HTML classes:", root.className);
        console.log("Body classes:", body.className);
        console.log("BG Color:", getComputedStyle(body).backgroundColor);
        console.log("Text Color:", getComputedStyle(body).color);
        console.log("==================");
    }, [theme]);

    const toggleTheme = () => {
        setTheme((prev) => {
            const newTheme = prev === "light" ? "dark" : "light";
            console.log(`🔄 Toggling theme: ${prev} → ${newTheme}`);
            return newTheme;
        });
    };

    return (
        <ThemeProviderContext.Provider value={{ theme, toggleTheme }}>
            {children}
        </ThemeProviderContext.Provider>
    );
}

export const useTheme = () => {
    const context = useContext(ThemeProviderContext);
    if (context === undefined) {
        throw new Error("useTheme must be used within a ThemeProvider");
    }
    return context;
};