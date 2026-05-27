'use client'

import { useState, useRef, useEffect } from 'react'
import { MessageCircle, X, Send, Loader2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ScrollArea } from '@/components/ui/scroll-area'

interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
}

export function ChatbotWidget() {
  const [isOpen, setIsOpen] = useState(false)
  const [messages, setMessages] = useState<Message[]>([
    {
      id: '1',
      role: 'assistant',
      content: 'Xin chào! 👋 Tôi là trợ lý tài chính AI của bạn. Hôm nay tôi có thể giúp gì cho bạn? Tôi có thể tư vấn về quản lý ngân sách, phân tích chi tiêu, hoặc trả lời các câu hỏi về tài chính cá nhân.',
      timestamp: new Date(),
    },
  ])
  const [inputValue, setInputValue] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const scrollAreaRef = useRef<HTMLDivElement>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!inputValue.trim() || isLoading) return

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: inputValue,
      timestamp: new Date(),
    }

    setMessages(prev => [...prev, userMessage])
    setInputValue('')
    setIsLoading(true)

    // Simulate AI response delay
    setTimeout(() => {
      const assistantMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: generateFinanceResponse(inputValue),
        timestamp: new Date(),
      }
      setMessages(prev => [...prev, assistantMessage])
      setIsLoading(false)
    }, 800)
  }

  const generateFinanceResponse = (userInput: string): string => {
    const lowercaseInput = userInput.toLowerCase()

    // Budget-related responses
    if (
      lowercaseInput.includes('ngân sách') ||
      lowercaseInput.includes('budget')
    ) {
      return 'Một ngân sách tốt là nền tảng của tài chính khỏe mạnh. Tôi khuyên bạn hãy áp dụng quy tắc 50/30/20: 50% cho nhu cầu thiết yếu, 30% cho mong muốn, và 20% cho tiết kiệm. Bạn có muốn biết thêm chi tiết về cách lập ngân sách không?'
    }

    // Saving-related responses
    if (
      lowercaseInput.includes('tiết kiệm') ||
      lowercaseInput.includes('tiết kiệm') ||
      lowercaseInput.includes('save')
    ) {
      return 'Tiết kiệm đều đặn là chìa khóa để xây dựng tài chính ổn định. Hãy bắt đầu với mục tiêu cụ thể và tạo một kế hoạch hàng tháng. Việc tiết kiệm ngay cả một khoản nhỏ cũng tốt hơn không tiết kiệm gì cả!'
    }

    // Income and expenses
    if (
      lowercaseInput.includes('chi tiêu') ||
      lowercaseInput.includes('expense') ||
      lowercaseInput.includes('thu nhập')
    ) {
      return 'Việc theo dõi chi tiêu giúp bạn hiểu rõ tiền đi đâu. Hãy phân loại chi tiêu của bạn thành các danh mục và xem xét những khoản có thể giảm. Ứng dụng của chúng tôi có thể giúp bạn trực quan hóa điều này!'
    }

    // Investment-related
    if (
      lowercaseInput.includes('đầu tư') ||
      lowercaseInput.includes('invest')
    ) {
      return 'Đầu tư là cách tuyệt vời để tăng trưởng tài chính. Trước khi đầu tư, hãy đảm bảo bạn đã có một quỹ khẩn cấp 3-6 tháng chi phí sống. Bắt đầu từ từ và học hỏi trước khi đầu tư số tiền lớn.'
    }

    // Debt-related
    if (
      lowercaseInput.includes('nợ') ||
      lowercaseInput.includes('debt') ||
      lowercaseInput.includes('vay')
    ) {
      return 'Quản lý nợ một cách thông minh là rất quan trọng. Hãy ưu tiên trả các khoản nợ lãi suất cao trước. Nếu có nhiều khoản nợ, xem xét phương pháp "tuyết lăn" hoặc "mưa tuyết" để trả nợ hiệu quả.'
    }

    // General questions
    if (
      lowercaseInput.includes('giúp') ||
      lowercaseInput.includes('tư vấn') ||
      lowercaseInput.includes('advice')
    ) {
      return 'Tôi sẵn lòng giúp bạn! Bạn có thể hỏi tôi về quản lý ngân sách, tiết kiệm, đầu tư, quản lý nợ, hoặc bất kỳ câu hỏi tài chính cá nhân nào. Hãy cụ thể hóa vấn đề của bạn để tôi có thể đưa ra lời khuyên tốt nhất.'
    }

    // Default response
    return 'Cảm ơn câu hỏi của bạn! Đó là một chủ đề thú vị liên quan đến tài chính. Tôi có thể giúp bạn hiểu rõ hơn và đưa ra chiến lược tốt nhất cho tình huống của bạn. Bạn có thể chia sẻ thêm chi tiết không?'
  }

  return (
    <>
      {/* Chat Widget */}
      <div
        className={`fixed bottom-6 right-6 z-40 transition-all duration-300 ${
          isOpen ? 'w-96' : 'w-auto'
        }`}
      >
        {isOpen ? (
          // Chat Window
          <div className="flex h-[600px] flex-col rounded-2xl border border-border bg-card shadow-lg">
            {/* Header */}
            <div className="flex items-center justify-between border-b border-border bg-gradient-to-r from-primary to-primary/90 px-5 py-4 text-primary-foreground rounded-t-2xl">
              <div className="flex items-center gap-2">
                <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary-foreground/20">
                  <MessageCircle className="h-5 w-5" />
                </div>
                <div className="flex flex-col">
                  <h3 className="font-semibold text-sm">Trợ lý Tài chính AI</h3>
                  <p className="text-xs opacity-90">Luôn sẵn lòng tư vấn</p>
                </div>
              </div>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setIsOpen(false)}
                className="h-8 w-8 text-primary-foreground hover:bg-primary-foreground/20"
              >
                <X className="h-5 w-5" />
              </Button>
            </div>

            {/* Messages Area */}
            <ScrollArea className="flex-1 p-4">
              <div className="space-y-4">
                {messages.map(message => (
                  <div
                    key={message.id}
                    className={`flex ${
                      message.role === 'user' ? 'justify-end' : 'justify-start'
                    }`}
                  >
                    <div
                      className={`max-w-xs rounded-lg px-4 py-2 text-sm ${
                        message.role === 'user'
                          ? 'bg-primary text-primary-foreground'
                          : 'bg-muted text-foreground'
                      }`}
                    >
                      <p className="whitespace-pre-wrap break-words">
                        {message.content}
                      </p>
                      <span
                        className={`mt-1 block text-xs opacity-70 ${
                          message.role === 'user'
                            ? 'text-primary-foreground'
                            : 'text-muted-foreground'
                        }`}
                      >
                        {message.timestamp.toLocaleTimeString('vi-VN', {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </span>
                    </div>
                  </div>
                ))}
                {isLoading && (
                  <div className="flex justify-start">
                    <div className="flex items-center gap-2 rounded-lg bg-muted px-4 py-3">
                      <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                      <span className="text-sm text-muted-foreground">
                        Đang suy nghĩ...
                      </span>
                    </div>
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>
            </ScrollArea>

            {/* Input Area */}
            <form
              onSubmit={handleSendMessage}
              className="border-t border-border p-4"
            >
              <div className="flex gap-2">
                <Input
                  value={inputValue}
                  onChange={e => setInputValue(e.target.value)}
                  placeholder="Hỏi tôi về tài chính..."
                  disabled={isLoading}
                  className="flex-1 text-sm"
                />
                <Button
                  type="submit"
                  size="icon"
                  disabled={isLoading || !inputValue.trim()}
                  className="bg-success hover:bg-success/90 text-success-foreground"
                >
                  {isLoading ? (
                    <Loader2 className="h-5 w-5 animate-spin" />
                  ) : (
                    <Send className="h-5 w-5" />
                  )}
                </Button>
              </div>
            </form>
          </div>
        ) : (
          // Floating Button
          <Button
            onClick={() => setIsOpen(true)}
            className="h-14 w-14 rounded-full bg-success hover:bg-success/90 text-success-foreground shadow-lg hover:shadow-xl transition-all duration-200 p-0"
          >
            <MessageCircle className="h-6 w-6" />
          </Button>
        )}
      </div>

      {/* Overlay (when open) */}
      {isOpen && (
        <div
          className="fixed inset-0 z-30 bg-black/20"
          onClick={() => setIsOpen(false)}
        />
      )}
    </>
  )
}
