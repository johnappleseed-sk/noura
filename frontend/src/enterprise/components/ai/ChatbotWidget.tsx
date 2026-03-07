import { FormEvent, useMemo, useState } from 'react'
import { useAppDispatch, useAppSelector } from '@/app/hooks'
import { selectChatbotOpen, toggleChatbot } from '@/features/ui/uiSlice'

interface ChatMessage {
  id: string
  sender: 'user' | 'bot'
  content: string
}

const cannedAnswers: Record<string, string> = {
  shipping: 'Standard shipping takes 3-5 business days, while express shipping takes 1-2 business days.',
  return: 'You can return products within 30 days. Go to Account > Order history > Start return.',
  payment: 'We support credit/debit cards, wallet payments, and Stripe secure checkout.',
  default: 'I can help with product recommendations, order status, shipping, and returns.',
}

const initialMessages: ChatMessage[] = [
  {
    id: 'bot-1',
    sender: 'bot',
    content: 'Hello, I am your AI support assistant. Ask about orders, products, shipping, or returns.',
  },
]

/**
 * Executes detect answer.
 *
 * @param input The input value.
 * @returns The result of detect answer.
 */
const detectAnswer = (input: string): string => {
  const value = input.toLowerCase()
  if (value.includes('ship')) {
    return cannedAnswers.shipping
  }
  if (value.includes('return')) {
    return cannedAnswers.return
  }
  if (value.includes('pay') || value.includes('stripe')) {
    return cannedAnswers.payment
  }
  return cannedAnswers.default
}

/**
 * Renders the ChatbotWidget component.
 *
 * @returns The rendered component tree.
 */
export const ChatbotWidget = (): JSX.Element => {
  const dispatch = useAppDispatch()
  const isOpen = useAppSelector(selectChatbotOpen)
  const [messages, setMessages] = useState<ChatMessage[]>(initialMessages)
  const [input, setInput] = useState('')

  const quickActions = useMemo(() => ['Shipping options', 'Return policy', 'Payment methods'], [])

  /**
   * Handles send message.
   */
  const sendMessage = (text: string): void => {
    if (!text.trim()) {
      return
    }
    const userMessage: ChatMessage = { id: `user-${Date.now()}`, sender: 'user', content: text }
    const botMessage: ChatMessage = {
      id: `bot-${Date.now()}`,
      sender: 'bot',
      content: detectAnswer(text),
    }
    setMessages((current) => [...current, userMessage, botMessage])
    setInput('')
  }

  /**
   * Executes submit message.
   *
   * @param event The event value.
   * @returns No value.
   */
  const submitMessage = (event: FormEvent<HTMLFormElement>): void => {
    event.preventDefault()
    sendMessage(input)
  }

  return (
    <div className="fixed bottom-4 right-4 z-50">
      {isOpen ? (
        <section
          aria-label="Customer support AI chatbot"
          className="panel-high mb-3 flex h-[460px] w-[320px] flex-col overflow-hidden"
        >
          <header className="flex items-center justify-between px-4 py-3" style={{ background: 'var(--m3-primary)', color: 'var(--m3-on-primary)' }}>
            <h2 className="text-sm font-semibold">AI Support</h2>
            <button className="text-xs font-semibold uppercase tracking-wide" onClick={() => dispatch(toggleChatbot())} type="button">
              Close
            </button>
          </header>

          <div className="flex-1 space-y-3 overflow-y-auto p-3">
            {messages.map((message) => (
              <div
                className={`max-w-[85%] rounded-xl px-3 py-2 text-sm ${
                  message.sender === 'bot'
                    ? 'bg-[color:var(--m3-surface-container-high)]'
                    : 'ml-auto'
                }`}
                style={
                  message.sender === 'user'
                    ? { background: 'var(--m3-primary)', color: 'var(--m3-on-primary)' }
                    : undefined
                }
                key={message.id}
              >
                {message.content}
              </div>
            ))}
          </div>

          <div className="border-t p-3" style={{ borderColor: 'var(--m3-outline-variant)' }}>
            <div className="mb-2 flex flex-wrap gap-2">
              {quickActions.map((action) => (
                <button
                  className="m3-chip"
                  key={action}
                  onClick={() => sendMessage(action)}
                  type="button"
                >
                  {action}
                </button>
              ))}
            </div>

            <form className="flex gap-2" onSubmit={submitMessage}>
              <input
                className="m3-input flex-1 !h-10 !rounded-xl !px-3 !py-1.5"
                onChange={(event) => setInput(event.target.value)}
                placeholder="Ask a question..."
                type="text"
                value={input}
              />
              <button className="m3-btn m3-btn-filled !h-10 !px-4 !py-1.5" type="submit">
                Send
              </button>
            </form>
          </div>
        </section>
      ) : null}

      <button
        aria-label="Toggle support chatbot"
        className="m3-btn m3-btn-filled !rounded-full !px-5 !py-3 text-sm shadow-lg"
        onClick={() => dispatch(toggleChatbot())}
        type="button"
      >
        AI Support
      </button>
    </div>
  )
}
