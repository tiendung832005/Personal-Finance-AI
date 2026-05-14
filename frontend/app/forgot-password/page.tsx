"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { apiFetch, ApiError } from "@/lib/api";
import { Sparkles, ArrowLeft, ArrowRight } from "lucide-react";

export default function ForgotPasswordPage() {
  const router = useRouter();
  const { toast } = useToast();

  const [step, setStep] = useState<"email" | "reset">("email");
  const [loading, setLoading] = useState(false);

  const [email, setEmail] = useState("");
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");

  const requestOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await apiFetch<void>("/api/auth/forgot-password/request-otp", {
        method: "POST",
        body: JSON.stringify({ email }),
        skipAuth: true,
      });
      setStep("reset");
      toast({ title: "Đã gửi OTP", description: `OTP đã gửi về ${email}` });
    } catch (err: any) {
      toast({
        title: "Không thể gửi OTP",
        description: err instanceof ApiError ? err.message : "Vui lòng thử lại",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const resetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await apiFetch<void>("/api/auth/forgot-password/verify-otp", {
        method: "POST",
        body: JSON.stringify({ email, otp, newPassword }),
        skipAuth: true,
      });
      toast({ title: "Thành công", description: "Mật khẩu đã được đặt lại." });
      router.push("/");
    } catch (err: any) {
      toast({
        title: "Đặt lại mật khẩu thất bại",
        description: err instanceof ApiError ? err.message : "Vui lòng thử lại",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen">
      <div className="flex w-full flex-col justify-center px-8 lg:w-1/2 lg:px-16 xl:px-24">
        <div className="mx-auto w-full max-w-md">
          <Link
            href="/"
            className="mb-8 inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="h-4 w-4" />
            Quay lại đăng nhập
          </Link>

          <div className="mb-8 flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary">
              <Sparkles className="h-5 w-5 text-primary-foreground" />
            </div>
            <span className="text-xl font-semibold text-foreground">
              Personal Finance Insight AI
            </span>
          </div>

          <div className="mb-8">
            <h1 className="text-2xl font-bold text-foreground">
              Quên mật khẩu
            </h1>
            <p className="mt-2 text-muted-foreground">
              {step === "email"
                ? "Nhập email để nhận OTP đặt lại mật khẩu."
                : "Nhập OTP và mật khẩu mới."}
            </p>
          </div>

          {step === "email" ? (
            <form onSubmit={requestOtp} className="space-y-5">
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="email@example.com"
                  required
                  className="h-11"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>

              <Button
                type="submit"
                className="h-11 w-full gap-2"
                disabled={loading}
              >
                {loading ? (
                  "Đang gửi OTP..."
                ) : (
                  <>
                    Gửi OTP
                    <ArrowRight className="h-4 w-4" />
                  </>
                )}
              </Button>
            </form>
          ) : (
            <form onSubmit={resetPassword} className="space-y-5">
              <div className="space-y-2">
                <Label htmlFor="otp">OTP (6 số)</Label>
                <Input
                  id="otp"
                  inputMode="numeric"
                  placeholder="123456"
                  required
                  className="h-11 tracking-widest"
                  value={otp}
                  onChange={(e) =>
                    setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))
                  }
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="newPassword">Mật khẩu mới</Label>
                <Input
                  id="newPassword"
                  type="password"
                  placeholder="Tối thiểu 8 ký tự"
                  required
                  minLength={8}
                  className="h-11"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                />
              </div>

              <Button
                type="submit"
                className="h-11 w-full"
                disabled={loading || otp.length !== 6}
              >
                {loading ? "Đang xác nhận..." : "Đặt lại mật khẩu"}
              </Button>

              <Button
                type="button"
                variant="outline"
                className="h-11 w-full"
                onClick={() => setStep("email")}
                disabled={loading}
              >
                Đổi email
              </Button>
            </form>
          )}
        </div>
      </div>

      <div className="hidden bg-secondary lg:flex lg:w-1/2 lg:flex-col lg:items-center lg:justify-center lg:p-12">
        <div className="max-w-lg text-center">
          <div className="mx-auto mb-8 flex h-32 w-32 items-center justify-center rounded-3xl bg-primary/10">
            <Sparkles className="h-16 w-16 text-primary" />
          </div>
          <h2 className="text-3xl font-bold text-foreground">
            Lấy lại quyền truy cập nhanh chóng
          </h2>
          <p className="mt-4 text-lg text-muted-foreground">
            OTP sẽ có hiệu lực trong thời gian ngắn. Nếu không nhận được email,
            hãy kiểm tra Spam.
          </p>
        </div>
      </div>
    </div>
  );
}
