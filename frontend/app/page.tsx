"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";
import { apiFetch, ApiError, decodeJwtPayload, setToken } from "@/lib/api";
import { Sparkles, Eye, EyeOff, ArrowRight } from "lucide-react";

export default function LoginPage() {
  const router = useRouter();
  const { toast } = useToast();

  const afterLoginPath = () => {
    if (typeof window === "undefined") return "/dashboard";
    const redirect = new URLSearchParams(window.location.search).get("redirect");
    return redirect && redirect.startsWith("/") ? redirect : "/dashboard";
  };
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [googleLoading, setGoogleLoading] = useState(false);
  const [googleStep, setGoogleStep] = useState<"idle" | "needOtp">("idle");
  const [googleIdToken, setGoogleIdToken] = useState<string | null>(null);
  const [googleOtp, setGoogleOtp] = useState("");
  const googleReadyRef = useRef(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      const res = await apiFetch<{
        token: string;
        tokenType: string;
        email: string;
        fullName: string;
      }>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
        skipAuth: true,
      });
      const token = res.data?.token;
      if (!token) throw new Error("Missing token");
      setToken(token);
      router.push(afterLoginPath());
    } catch (err: any) {
      const message =
        err instanceof ApiError ? err.message : "Đăng nhập thất bại";
      toast({
        title: "Đăng nhập thất bại",
        description: message,
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };

  const ensureGoogleScript = async () => {
    if (typeof window === "undefined") return;
    if ((window as any).google?.accounts?.id) return;
    await new Promise<void>((resolve, reject) => {
      const script = document.createElement("script");
      script.src = "https://accounts.google.com/gsi/client";
      script.async = true;
      script.defer = true;
      script.onload = () => resolve();
      script.onerror = () => reject(new Error("Cannot load Google script"));
      document.head.appendChild(script);
    });
  };

  const googleAuthWithIdToken = async (idToken: string) => {
    const authRes = await apiFetch<{
      token: string;
      tokenType: string;
      email: string;
      fullName: string;
    }>("/api/auth/google", {
      method: "POST",
      body: JSON.stringify({ idToken }),
      skipAuth: true,
    });

    const token = authRes.data?.token;
    if (!token) throw new Error("Missing token");
    setToken(token);
    router.push(afterLoginPath());
  };

  const initGoogle = async () => {
    const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
    if (!clientId) return;
    if (googleReadyRef.current) return;

    await ensureGoogleScript();
    const google = (window as any).google;
    if (!google?.accounts?.id) throw new Error("Google SDK not available");

    google.accounts.id.initialize({
      client_id: clientId,
      ux_mode: "popup",
      callback: async (resp: any) => {
        const idToken = resp?.credential;
        if (!idToken) {
          toast({
            title: "Google đăng nhập thất bại",
            description: "Không nhận được credential từ Google.",
            variant: "destructive",
          });
          return;
        }

        // keep for OTP step
        setGoogleIdToken(idToken);

        try {
          await googleAuthWithIdToken(idToken);
        } catch (err: any) {
          const msg =
            err instanceof ApiError ? err.message : String(err?.message || err);

          if (msg.toLowerCase().includes("otp is required")) {
            try {
              const emailFromToken = decodeJwtPayload(idToken)?.email;
              if (!emailFromToken) {
                throw new Error("Không lấy được email từ Google token");
              }
              await apiFetch<void>("/api/auth/google/request-otp", {
                method: "POST",
                body: JSON.stringify({ email: emailFromToken }),
                skipAuth: true,
              });
              setGoogleStep("needOtp");
              toast({
                title: "Đã gửi OTP",
                description: `OTP đã gửi về ${emailFromToken}`,
              });
            } catch (e: any) {
              toast({
                title: "Không thể gửi OTP",
                description:
                  e instanceof ApiError ? e.message : "Vui lòng thử lại",
                variant: "destructive",
              });
            }
          } else {
            toast({
              title: "Google đăng nhập thất bại",
              description: msg,
              variant: "destructive",
            });
          }
        } finally {
          setGoogleLoading(false);
        }
      },
    });

    googleReadyRef.current = true;
  };

  useEffect(() => {
    // Preload/init once (so click feels instant)
    initGoogle().catch(() => {
      // silent: will show error on click
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleGoogleSignIn = async () => {
    const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID;
    if (!clientId) {
      toast({
        title: "Thiếu cấu hình Google",
        description: "Cần set NEXT_PUBLIC_GOOGLE_CLIENT_ID ở frontend.",
        variant: "destructive",
      });
      return;
    }

    setGoogleLoading(true);
    try {
      await initGoogle();
      const google = (window as any).google;
      if (!google?.accounts?.id) {
        throw new Error("Google SDK not available");
      }

      google.accounts.id.prompt((notification: any) => {
        if (notification?.isNotDisplayed?.()) {
          setGoogleLoading(false);
          toast({
            title: "Không mở được Google",
            description:
              "Google prompt không hiển thị. Hãy kiểm tra popup/cookie hoặc thử lại bằng trình duyệt Chrome.",
            variant: "destructive",
          });
        }
        if (notification?.isSkippedMoment?.()) {
          setGoogleLoading(false);
        }
      });
    } catch (err: any) {
      const msg =
        err instanceof ApiError ? err.message : String(err?.message || err);
      toast({
        title: "Google đăng nhập thất bại",
        description: msg,
        variant: "destructive",
      });
    } finally  {
      // googleLoading will be turned off by notification/callback as needed
    }
  };

  const handleGoogleVerifyOtp = async () => {
    if (!googleIdToken) return;
    setGoogleLoading(true);
    try {
      const authRes = await apiFetch<{
        token: string;
        tokenType: string;
        email: string;
        fullName: string;
      }>("/api/auth/google", {
        method: "POST",
        body: JSON.stringify({ idToken: googleIdToken, otp: googleOtp }),
        skipAuth: true,
      });
      const token = authRes.data?.token;
      if (!token) throw new Error("Missing token");
      setToken(token);
      router.push(afterLoginPath());
    } catch (err: any) {
      toast({
        title: "OTP không hợp lệ",
        description: err instanceof ApiError ? err.message : "Vui lòng thử lại",
        variant: "destructive",
      });
    } finally {
      setGoogleLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen">
      {/* Left side - Form */}
      <div className="flex w-full flex-col justify-center px-8 lg:w-1/2 lg:px-16 xl:px-24">
        <div className="mx-auto w-full max-w-md">
          {/* Logo */}
          <div className="mb-8 flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary">
              <Sparkles className="h-5 w-5 text-primary-foreground" />
            </div>
            <span className="text-xl font-semibold text-foreground">
              Personal Finance Insight AI
            </span>
          </div>

          {/* Welcome text */}
          <div className="mb-8">
            <h1 className="text-2xl font-bold text-foreground">
              Chào mừng trở lại
            </h1>
            <p className="mt-2 text-muted-foreground">
              Đăng nhập để quản lý tài chính của bạn
            </p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-5">
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

            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="password">Mật khẩu</Label>
                <Link
                  href="/forgot-password"
                  className="text-sm text-muted-foreground hover:text-foreground"
                >
                  Quên mật khẩu?
                </Link>
              </div>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="Nhập mật khẩu"
                  required
                  className="h-11 pr-10"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                >
                  {showPassword ? (
                    <EyeOff className="h-4 w-4" />
                  ) : (
                    <Eye className="h-4 w-4" />
                  )}
                </button>
              </div>
            </div>

            <Button
              type="submit"
              className="h-11 w-full gap-2"
              disabled={isLoading}
            >
              {isLoading ? (
                "Đang đăng nhập..."
              ) : (
                <>
                  Đăng nhập
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </Button>
          </form>

          {/* Google auth */}
          <div className="mt-5 space-y-3">
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <span className="w-full border-t border-border" />
              </div>
              <div className="relative flex justify-center text-xs uppercase">
                <span className="bg-background px-2 text-muted-foreground">
                  hoặc
                </span>
              </div>
            </div>

            {googleStep === "idle" ? (
              <>
                <Button
                  type="button"
                  variant="outline"
                  className="h-11 w-full"
                  disabled={googleLoading}
                  onClick={handleGoogleSignIn}
                >
                  {googleLoading ? "Đang mở Google..." : "Tiếp tục với Google"}
                </Button>
                <div id="googleBtn" className="hidden" />
              </>
            ) : (
              <div className="space-y-3 rounded-lg border border-border bg-card p-4">
                <p className="text-sm text-muted-foreground">
                  Nhập OTP đã gửi về email để hoàn tất đăng ký bằng Google.
                </p>
                <div className="space-y-2">
                  <Label htmlFor="googleOtp">OTP (6 số)</Label>
                  <Input
                    id="googleOtp"
                    inputMode="numeric"
                    placeholder="123456"
                    value={googleOtp}
                    onChange={(e) =>
                      setGoogleOtp(
                        e.target.value.replace(/\D/g, "").slice(0, 6),
                      )
                    }
                    className="h-11 tracking-widest"
                  />
                </div>
                <Button
                  type="button"
                  className="h-11 w-full"
                  disabled={googleLoading || googleOtp.length !== 6}
                  onClick={handleGoogleVerifyOtp}
                >
                  {googleLoading ? "Đang xác thực..." : "Xác thực OTP"}
                </Button>
              </div>
            )}
          </div>

          {/* Register link */}
          <p className="mt-6 text-center text-sm text-muted-foreground">
            Chưa có tài khoản?{" "}
            <Link
              href="/register"
              className="font-medium text-foreground hover:underline"
            >
              Đăng ký ngay
            </Link>
          </p>
        </div>
      </div>

      {/* Right side - Illustration */}
      <div className="hidden bg-secondary lg:flex lg:w-1/2 lg:flex-col lg:items-center lg:justify-center lg:p-12">
        <div className="max-w-lg text-center">
          <div className="mx-auto mb-8 flex h-32 w-32 items-center justify-center rounded-3xl bg-primary/10">
            <Sparkles className="h-16 w-16 text-primary" />
          </div>
          <h2 className="text-3xl font-bold text-foreground">
            Quản lý tài chính thông minh với AI
          </h2>
          <p className="mt-4 text-lg text-muted-foreground">
            Theo dõi thu chi, phân tích xu hướng và nhận gợi ý tiết kiệm thông
            minh từ trí tuệ nhân tạo.
          </p>

          {/* Feature highlights */}
          <div className="mt-10 grid gap-4 text-left">
            {[
              "Tự động phân loại giao dịch",
              "Phân tích chi tiêu theo danh mục",
              "Dự báo và cảnh báo thông minh",
              "Quản lý tài chính gia đình",
            ].map((feature, i) => (
              <div
                key={i}
                className="flex items-center gap-3 rounded-lg bg-card px-4 py-3 shadow-sm"
              >
                <div className="flex h-6 w-6 items-center justify-center rounded-full bg-success/10 text-success">
                  <svg
                    className="h-3.5 w-3.5"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    strokeWidth={3}
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      d="M5 13l4 4L19 7"
                    />
                  </svg>
                </div>
                <span className="text-sm font-medium text-foreground">
                  {feature}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
