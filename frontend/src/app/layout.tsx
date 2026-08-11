import type { Metadata } from "next";
import { DM_Sans, IBM_Plex_Mono } from "next/font/google";
import { Toaster } from "sonner";
import { QueryProvider } from "@/providers/QueryProvider";
import { AuthProvider } from "@/providers/AuthProvider";
import { BagProvider } from "@/providers/BagProvider";
import { AppModeProvider } from "@/providers/AppModeProvider";
import "./globals.css";

const dmSans = DM_Sans({
  subsets: ["latin"],
  variable: "--font-dm-sans",
  display: "swap",
  weight: "variable",
  style: ["normal", "italic"],
  axes: ["opsz"],
});

const ibmPlexMono = IBM_Plex_Mono({
  subsets: ["latin"],
  weight: ["400", "500", "600"],
  variable: "--font-ibm-plex-mono",
  display: "swap",
});

export const metadata: Metadata = {
  title: { default: "Closiq — Buy · Rent · Redefine", template: "%s — Closiq" },
  description: "Premium clothing rental marketplace with 15-minute home trial.",
  icons: {
    icon: "/logo-icon.png",
    apple: "/logo-icon.png",
  },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html
      lang="en"
      className={`${dmSans.variable} ${ibmPlexMono.variable} h-full`}
    >
      <body className="min-h-full flex flex-col antialiased font-sans">
        <QueryProvider>
          <AuthProvider>
            <BagProvider>
              <AppModeProvider>
                {children}
                <Toaster position="top-center" richColors />
              </AppModeProvider>
            </BagProvider>
          </AuthProvider>
        </QueryProvider>
      </body>
    </html>
  );
}
