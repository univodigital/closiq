import Link from "next/link";
import { Logo } from "@/shared/components/layout/Logo";

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-5 py-12">
      <div className="flex w-full max-w-md flex-col items-center">
        <Logo href="/" size="hero" priority className="mb-5" />
        {children}
      </div>
    </div>
  );
}
