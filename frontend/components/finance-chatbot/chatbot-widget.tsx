'use client'

import { useState, useRef, useEffect } from 'react'
import { MessageCircle, X, Send, Loader2, RotateCcw } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { apiFetch } from '@/lib/api'


interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
}

export function ChatbotWidget() {
  const [isOpen, setIsOpen] = useState(false)
  const [messages, setMessages] = useState<Message[]>([])
  
  const [inputValue, setInputValue] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const scrollAreaRef = useRef<HTMLDivElement>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  // Load messages from localStorage on mount
  useEffect(() => {
    const savedData = localStorage.getItem('pfia.chat_history')
    if (savedData) {
      try {
        const { messages: savedMessages, lastUpdated } = JSON.parse(savedData)
        
        // Check if history is older than 24 hours
        const twentyFourHours = 24 * 60 * 60 * 1000
        const isExpired = Date.now() - lastUpdated > twentyFourHours

        if (isExpired) {
          resetToDefaultMessages()
        } else {
          const messagesWithDates = savedMessages.map((msg: any) => ({
            ...msg,
            timestamp: new Date(msg.timestamp)
          }))
          setMessages(messagesWithDates)
        }
      } catch (e) {
        console.error('Failed to parse chat history', e)
        resetToDefaultMessages()
      }
    } else {
      resetToDefaultMessages()
    }
  }, [])

  // Save messages to localStorage whenever they change
  useEffect(() => {
    if (messages.length > 0) {
      const dataToSave = {
        messages,
        lastUpdated: Date.now()
      }
      localStorage.setItem('pfia.chat_history', JSON.stringify(dataToSave))
    }
  }, [messages])

  const resetToDefaultMessages = () => {
    localStorage.removeItem('pfia.chat_history')
    setMessages([
      {
        id: '1',
        role: 'assistant',
        content: 'Xin chào! 👋 Tôi là trợ lý tài chính AI của bạn. Lưu ý: Lịch sử chat của bạn sẽ được lưu giữ trong vòng 24 giờ kể từ tin nhắn cuối cùng để hỗ trợ tư vấn tốt nhất.',
        timestamp: new Date(),
      },
    ])
  }

  const handleNewChat = () => {
    if (window.confirm('Bạn có muốn xóa lịch sử chat hiện tại và bắt đầu cuộc hội thoại mới?')) {
      resetToDefaultMessages()
    }
  }


  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  const handleSendMessage = async (e?: React.FormEvent, manualContent?: string) => {
    e?.preventDefault()
    const content = manualContent || inputValue
    if (!content.trim() || isLoading) return

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: content,
      timestamp: new Date(),
    }

    setMessages(prev => [...prev, userMessage])
    setInputValue('')
    setIsLoading(true)

    try {
      const chatHistory = messages.map(msg => ({
        role: msg.role,
        content: msg.content
      }))
      
      const payload = {
        model: 'gemini-1.5-flash',
        messages: [...chatHistory, { role: 'user', content }],
        max_tokens: 500,
        temperature: 0.7
      }


      const response = await apiFetch<string>('/api/ai/chat', {
        method: 'POST',
        body: JSON.stringify(payload),
      })

      const assistantMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: response.success ? (response.data || '') : (response.message || 'Có lỗi xảy ra khi kết nối với AI.'),
        timestamp: new Date(),
      }
      setMessages(prev => [...prev, assistantMessage])
    } catch (error: any) {
      console.error('Chat error:', error)
      const assistantMessage: Message = {
        id: (Date.now() + 1).toString(),
        role: 'assistant',
        content: error.message || 'Xin lỗi, tôi đang gặp khó khăn khi kết nối. Vui lòng thử lại sau.',
        timestamp: new Date(),
      }
      setMessages(prev => [...prev, assistantMessage])
    } finally {
      setIsLoading(false)
    }
  }

  const quickSuggestions = [
    { label: 'Tiết kiệm 💡', value: 'Làm thế nào để tiết kiệm tiền hiệu quả?' },
    { label: 'Quy tắc 50/30/20 📊', value: 'Giải thích quy tắc 50/30/20 trong quản lý tài chính' },
    { label: 'Đầu tư 📈', value: 'Tôi nên bắt đầu đầu tư từ đâu?' },
    { label: 'Quản lý nợ 💸', value: 'Làm thế nào để thoát khỏi nợ nần nhanh chóng?' },
  ]

  return (

    <>
      {/* Chat Widget */}
      <div
        className={`fixed bottom-6 right-6 z-40 transition-all duration-300 ${
          isOpen ? 'w-[calc(100vw-48px)] sm:w-96' : 'w-auto'
        }`}

      >
        {isOpen ? (
          // Chat Window
          <div className="flex h-[600px] max-h-[calc(100vh-120px)] w-full flex-col rounded-2xl border border-border bg-card shadow-2xl overflow-hidden animate-in slide-in-from-bottom-4 duration-300">

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
              <div className="flex items-center gap-1">
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={handleNewChat}
                  title="Bắt đầu đoạn chat mới"
                  className="h-8 w-8 text-primary-foreground hover:bg-primary-foreground/20"
                >
                  <RotateCcw className="h-4 w-4" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => setIsOpen(false)}
                  className="h-8 w-8 text-primary-foreground hover:bg-primary-foreground/20"
                >
                  <X className="h-5 w-5" />
                </Button>
              </div>
            </div>

            {/* Messages Area */}
            <div className="flex-1 overflow-y-auto p-4 scrollbar-thin scrollbar-thumb-primary/20 scrollbar-track-transparent hover:scrollbar-thumb-primary/30 transition-colors">
              <div className="space-y-4">
                {messages.map(message => (
                  <div
                    key={message.id}
                    className={`flex ${
                      message.role === 'user' ? 'justify-end' : 'justify-start'
                    }`}
                  >
                    <div
                      className={`max-w-[85%] rounded-2xl px-4 py-2.5 text-sm shadow-sm ${
                        message.role === 'user'
                          ? 'bg-primary text-primary-foreground rounded-tr-none'
                          : 'bg-muted text-foreground rounded-tl-none'
                      }`}
                    >
                      <p className="whitespace-pre-wrap break-words leading-relaxed">
                        {message.content}
                      </p>
                      <span
                        className={`mt-1.5 block text-[10px] opacity-60 ${
                          message.role === 'user'
                            ? 'text-primary-foreground text-right'
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
                    <div className="flex items-center gap-3 rounded-2xl bg-muted px-4 py-3 rounded-tl-none shadow-sm">
                      <Loader2 className="h-4 w-4 animate-spin text-primary" />
                      <span className="text-xs text-muted-foreground font-medium">
                        Đang phân tích...
                      </span>
                    </div>
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>
            </div>


            {/* Quick Suggestions */}
            {messages.length <= 2 && !isLoading && (
              <div className="px-4 py-3 border-t border-border bg-muted/20">
                <p className="text-[10px] uppercase font-bold text-muted-foreground mb-2 px-1 tracking-wider">
                  Gợi ý câu hỏi
                </p>
                <div className="flex flex-wrap gap-2">
                  {quickSuggestions.map((suggestion) => (
                    <button
                      key={suggestion.label}
                      onClick={() => handleSendMessage(undefined, suggestion.value)}
                      className="text-xs px-3 py-2 rounded-xl bg-background border border-border hover:border-primary hover:bg-primary/5 hover:text-primary transition-all text-left shadow-sm active:scale-95"
                    >
                      {suggestion.label}
                    </button>
                  ))}
                </div>
              </div>
            )}


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