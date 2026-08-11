"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/providers/AuthProvider";
import { ROUTES } from "@/shared/constants/routes";
import { maskPhone } from "@/lib/phone-mask";
import { ApiError } from "@/lib/api-client";
import {
  changePassword,
  changeUsername,
  completePhoneChange,
  confirmAvatarUpload,
  createAvatarUploadUrl,
  initiatePhoneChange,
  removeAvatar,
  requestEmailChange,
  resendAccountOtp,
  sendNewPhoneOtp,
  verifyEmailChange,
  verifyOldPhoneOtp,
} from "@/features/user/services/account-security.service";
import { uploadAvatarBinary, validateAvatarFile } from "@/features/user/lib/avatar-upload";

type PhoneStep = "idle" | "old-otp" | "new-phone" | "new-otp" | "done";

export default function AccountSecurityPage() {
  const { user, refreshUser } = useAuth();

  const [phoneStep, setPhoneStep] = useState<PhoneStep>("idle");
  const [oldSessionId, setOldSessionId] = useState("");
  const [newSessionId, setNewSessionId] = useState("");
  const [oldOtp, setOldOtp] = useState("");
  const [newPhone, setNewPhone] = useState("");
  const [newOtp, setNewOtp] = useState("");
  const [resendIn, setResendIn] = useState(0);
  const [phoneLoading, setPhoneLoading] = useState(false);

  const [newEmail, setNewEmail] = useState("");
  const [emailSessionId, setEmailSessionId] = useState("");
  const [emailOtp, setEmailOtp] = useState("");
  const [emailLoading, setEmailLoading] = useState(false);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [passwordLoading, setPasswordLoading] = useState(false);

  const [usernameInput, setUsernameInput] = useState(user?.username ?? "");
  const [usernameLoading, setUsernameLoading] = useState(false);

  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarLoading, setAvatarLoading] = useState(false);

  useEffect(() => {
    if (resendIn <= 0) return;
    const t = window.setInterval(() => setResendIn((s) => Math.max(0, s - 1)), 1000);
    return () => window.clearInterval(t);
  }, [resendIn]);

  async function handleResend(sessionId: string) {
    if (!sessionId || resendIn > 0) return;
    const result = await resendAccountOtp(sessionId);
    setResendIn(result.resendAvailableInSeconds);
    toast.success("OTP resent");
  }

  async function startPhoneChange() {
    setPhoneLoading(true);
    try {
      const result = await initiatePhoneChange();
      setOldSessionId(result.otpSessionId);
      setPhoneStep("old-otp");
      setResendIn(result.resendAvailableInSeconds);
      toast.success(`OTP sent to ${maskPhone(user?.phone ?? result.phone ?? "")}`);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Could not send OTP");
    } finally {
      setPhoneLoading(false);
    }
  }

  async function submitOldOtp() {
    setPhoneLoading(true);
    try {
      await verifyOldPhoneOtp(oldSessionId, oldOtp);
      setPhoneStep("new-phone");
      toast.success("Current phone verified");
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Invalid OTP");
    } finally {
      setPhoneLoading(false);
    }
  }

  async function submitNewPhone() {
    setPhoneLoading(true);
    try {
      const phone = newPhone.startsWith("+91") ? newPhone : `+91${newPhone}`;
      const result = await sendNewPhoneOtp(phone);
      setNewSessionId(result.otpSessionId);
      setPhoneStep("new-otp");
      setResendIn(result.resendAvailableInSeconds);
      toast.success(`OTP sent to ${maskPhone(phone)}`);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Could not send OTP to new number");
    } finally {
      setPhoneLoading(false);
    }
  }

  async function submitNewOtp() {
    setPhoneLoading(true);
    try {
      await completePhoneChange(newSessionId, newOtp);
      setPhoneStep("done");
      toast.success("Phone number updated. Please sign in again on other devices.");
      await refreshUser();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Could not update phone");
    } finally {
      setPhoneLoading(false);
    }
  }

  async function submitEmailChange() {
    setEmailLoading(true);
    try {
      const result = await requestEmailChange(newEmail.trim());
      setEmailSessionId(result.otpSessionId);
      setResendIn(result.resendAvailableInSeconds);
      toast.success("Verification code sent to your new email");
      await refreshUser();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Could not request email change");
    } finally {
      setEmailLoading(false);
    }
  }

  async function submitEmailOtp() {
    setEmailLoading(true);
    try {
      await verifyEmailChange(emailSessionId, emailOtp);
      setEmailOtp("");
      setEmailSessionId("");
      toast.success("Email updated");
      await refreshUser();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Invalid OTP");
    } finally {
      setEmailLoading(false);
    }
  }

  async function submitPassword() {
    setPasswordLoading(true);
    try {
      await changePassword(currentPassword, newPassword, confirmPassword);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      toast.success("Password updated. Other devices have been signed out.");
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Could not change password");
    } finally {
      setPasswordLoading(false);
    }
  }

  async function submitUsername() {
    setUsernameLoading(true);
    try {
      await changeUsername(usernameInput.trim());
      toast.success("Username updated");
      await refreshUser();
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Could not change username");
    } finally {
      setUsernameLoading(false);
    }
  }

  function onAvatarSelect(file: File | null) {
    if (!file) return;
    const err = validateAvatarFile(file);
    if (err) {
      toast.error(err);
      return;
    }
    setAvatarFile(file);
    setAvatarPreview(URL.createObjectURL(file));
  }

  async function uploadAvatar() {
    if (!avatarFile) return;
    setAvatarLoading(true);
    try {
      const instruction = await createAvatarUploadUrl(avatarFile.name, avatarFile.type, avatarFile.size);
      await uploadAvatarBinary(instruction, avatarFile);
      await confirmAvatarUpload(instruction.uploadId);
      toast.success("Avatar updated");
      setAvatarFile(null);
      setAvatarPreview(null);
      await refreshUser();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : "Avatar upload failed");
    } finally {
      setAvatarLoading(false);
    }
  }

  async function handleRemoveAvatar() {
    setAvatarLoading(true);
    try {
      await removeAvatar();
      toast.success("Avatar removed");
      await refreshUser();
    } catch (e) {
      toast.error("Could not remove avatar");
    } finally {
      setAvatarLoading(false);
    }
  }

  return (
    <Container narrow embedded>
      <PageHeader
        title="Security & profile"
        description="Manage your avatar, contact details, password, and username."
      />

      <section className="mb-8 space-y-4 rounded-sm border border-border p-4">
        <h2 className="font-medium">Profile</h2>
        <div className="flex items-center gap-4">
          {avatarPreview || user?.avatarUrl ? (
            // eslint-disable-next-line @next/next/no-img-element
            <img
              src={avatarPreview ?? user?.avatarUrl ?? ""}
              alt="Avatar preview"
              className="h-16 w-16 rounded-full object-cover"
            />
          ) : (
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-muted text-lg font-medium">
              {user?.firstName?.charAt(0) ?? "?"}
            </div>
          )}
          <div className="space-y-2">
            <Input type="file" accept="image/jpeg,image/png,image/webp" onChange={(e) => onAvatarSelect(e.target.files?.[0] ?? null)} />
            <div className="flex gap-2">
              <Button size="sm" variant="primary" disabled={!avatarFile || avatarLoading} onClick={uploadAvatar}>
                {avatarLoading ? "Uploading…" : "Save avatar"}
              </Button>
              {user?.avatarUrl && (
                <Button size="sm" variant="outline" disabled={avatarLoading} onClick={handleRemoveAvatar}>
                  Remove
                </Button>
              )}
            </div>
          </div>
        </div>

        <div>
          <label className="label-caps mb-2 block text-muted-foreground">Username</label>
          <Input value={usernameInput} onChange={(e) => setUsernameInput(e.target.value)} disabled={!user?.usernameChangeAllowed} />
          <p className="mt-1 text-xs text-muted-foreground">
            {user?.usernameChangeAllowed
              ? "You can change your username once."
              : "Username changes are no longer available."}
          </p>
          {user?.usernameChangeAllowed && (
            <Button className="mt-2" size="sm" variant="outline" disabled={usernameLoading} onClick={submitUsername}>
              Update username
            </Button>
          )}
        </div>
      </section>

      <section className="mb-8 space-y-3 rounded-sm border border-border p-4">
        <h2 className="font-medium">Phone number</h2>
        <p className="text-sm text-muted-foreground">Current: {maskPhone(user?.phone ?? "")}</p>
        {phoneStep === "idle" && (
          <Button variant="outline" size="sm" disabled={phoneLoading} onClick={startPhoneChange}>
            Change phone number
          </Button>
        )}
        {phoneStep === "old-otp" && (
          <div className="space-y-2">
            <p className="text-sm">Enter OTP sent to {maskPhone(user?.phone ?? "")}</p>
            <Input value={oldOtp} onChange={(e) => setOldOtp(e.target.value.replace(/\D/g, "").slice(0, 6))} placeholder="6-digit OTP" />
            <Button size="sm" onClick={submitOldOtp} disabled={phoneLoading || oldOtp.length !== 6}>Verify current phone</Button>
            {resendIn > 0 ? <p className="text-xs text-muted-foreground">Resend in {resendIn}s</p> : (
              <Button variant="ghost" size="sm" onClick={() => handleResend(oldSessionId)}>Resend OTP</Button>
            )}
          </div>
        )}
        {phoneStep === "new-phone" && (
          <div className="space-y-2">
            <Input value={newPhone} onChange={(e) => setNewPhone(e.target.value)} placeholder="New 10-digit mobile" />
            <Button size="sm" onClick={submitNewPhone} disabled={phoneLoading}>Send OTP to new number</Button>
          </div>
        )}
        {phoneStep === "new-otp" && (
          <div className="space-y-2">
            <Input value={newOtp} onChange={(e) => setNewOtp(e.target.value.replace(/\D/g, "").slice(0, 6))} placeholder="OTP for new number" />
            <Button size="sm" onClick={submitNewOtp} disabled={phoneLoading || newOtp.length !== 6}>Confirm new phone</Button>
            {resendIn > 0 ? <p className="text-xs text-muted-foreground">Resend in {resendIn}s</p> : (
              <Button variant="ghost" size="sm" onClick={() => handleResend(newSessionId)}>Resend OTP</Button>
            )}
          </div>
        )}
        {phoneStep === "done" && <p className="text-sm text-success">Phone number updated successfully.</p>}
      </section>

      <section className="mb-8 space-y-3 rounded-sm border border-border p-4">
        <h2 className="font-medium">Email</h2>
        <p className="text-sm text-muted-foreground">
          Current: {user?.email ?? "Not set"} {user?.emailVerified ? "(verified)" : "(unverified)"}
        </p>
        {user?.pendingEmail && (
          <p className="text-sm text-warning">Pending verification: {user.pendingEmail}</p>
        )}
        <Input type="email" value={newEmail} onChange={(e) => setNewEmail(e.target.value)} placeholder="New email address" />
        <Button size="sm" variant="outline" disabled={emailLoading} onClick={submitEmailChange}>
          Send verification to new email
        </Button>
        {emailSessionId && (
          <div className="space-y-2 pt-2">
            <Input value={emailOtp} onChange={(e) => setEmailOtp(e.target.value.replace(/\D/g, "").slice(0, 6))} placeholder="Verification code" />
            <Button size="sm" onClick={submitEmailOtp} disabled={emailLoading || emailOtp.length !== 6}>Verify email</Button>
          </div>
        )}
      </section>

      <section className="mb-8 space-y-3 rounded-sm border border-border p-4">
        <h2 className="font-medium">Password</h2>
        <Input type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} placeholder="Current password" />
        <Input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} placeholder="New password" />
        <Input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} placeholder="Confirm new password" />
        <Button size="sm" variant="primary" disabled={passwordLoading} onClick={submitPassword}>
          Change password
        </Button>
        <p className="text-xs text-muted-foreground">Other signed-in devices will be signed out.</p>
      </section>

      <p className="text-sm text-muted-foreground">
        <Link href={ROUTES.account.settings} className="text-accent hover:underline">Back to settings</Link>
      </p>
    </Container>
  );
}
