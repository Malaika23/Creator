import React, { useState, useRef, useEffect } from 'react';

export default function AiAssistant({ products, isBackendConnected, apiGateway }) {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([
    { id: 1, text: "Hi! I am your AI Shopping Assistant. Ask me anything like: 'Show me gaming laptops' or 'What mechanical keyboards do you have?'", assistant: true }
  ]);
  const [input, setInput] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const chatEndRef = useRef(null);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isTyping]);

  const handleSend = (e) => {
    e.preventDefault();
    if (!input.trim()) return;

    const userText = input.trim();
    setMessages(prev => [...prev, { id: Date.now(), text: userText, assistant: false }]);
    setInput("");
    setIsTyping(true);

    if (isBackendConnected) {
      // Query the actual backend AI Service endpoint
      fetch(`${apiGateway}/api/v1/assistant/chat?message=${encodeURIComponent(userText)}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
      })
      .then(res => res.json())
      .then(data => {
        setIsTyping(false);
        setMessages(prev => [...prev, { id: Date.now(), text: data.reply || data.response || "I couldn't fetch an answer from the model.", assistant: true }]);
      })
      .catch(err => {
        console.warn("AI service call failed, falling back to local simulation:", err);
        handleOfflineAiResponse(userText);
      });
    } else {
      // Simulate offline AI response
      setTimeout(() => {
        handleOfflineAiResponse(userText);
      }, 1000);
    }
  };

  const handleOfflineAiResponse = (query) => {
    setIsTyping(false);
    const text = query.toLowerCase();
    let reply = "";

    // Keyword logic for local search simulation
    if (text.includes("gaming") || text.includes("laptop")) {
      const laptop = products.find(p => p.id === 1);
      reply = `I found a great match! The **${laptop.name}** is an ultra-slim gaming laptop powered by AMD Ryzen 9 and an NVIDIA RTX 4060 graphics card, available for **$${laptop.price}**. Would you like me to add it to your cart?`;
    } else if (text.includes("headphone") || text.includes("audio") || text.includes("sound")) {
      const headphones = products.find(p => p.id === 2);
      reply = `You might like the **${headphones.name}**! They feature active noise cancellation (ANC), premium Hi-Res audio drivers, and up to 40 hours of battery life. They are priced at **$${headphones.price}**.`;
    } else if (text.includes("watch") || text.includes("fitness") || text.includes("smartwatch")) {
      const watch = products.find(p => p.id === 3);
      reply = `Check out the **${watch.name}** for **$${watch.price}**. It comes with continuous heart-rate tracking, blood oxygen levels (SpO2), and integrated GPS mapping.`;
    } else if (text.includes("keyboard") || text.includes("mechanical")) {
      const keyboard = products.find(p => p.id === 4);
      reply = `Yes, we have the **${keyboard.name}**. It's hot-swappable with linear switches, vibrant RGB lighting, and dual-shot keycaps for **$${keyboard.price}**.`;
    } else if (text.includes("monitor") || text.includes("screen") || text.includes("display")) {
      const monitor = products.find(p => p.id === 5);
      reply = `The **${monitor.name}** is excellent for work and gaming. It has a 27" QHD screen with an ultra-fast 170Hz refresh rate and HDR support, priced at **$${monitor.price}**.`;
    } else if (text.includes("charge") || text.includes("wireless") || text.includes("pad")) {
      const charger = products.find(p => p.id === 6);
      reply = `We have the **${charger.name}** for **$${charger.price}**. It supports 15W fast wireless charging for your smartphone or wireless earbuds.`;
    } else if (text.includes("hello") || text.includes("hi") || text.includes("hey")) {
      reply = "Hello! I am your AI assistant. Tell me what product or category you are interested in, and I will recommend the best option!";
    } else {
      reply = "I understand you are looking for shopping help. Try searching for keywords like **gaming laptop**, **headphones**, **wireless charger**, or **mechanical keyboard** to get customized product insights!";
    }

    setMessages(prev => [...prev, { id: Date.now(), text: reply, assistant: true }]);
  };

  return (
    <>
      <button className="ai-assistant-trigger" onClick={() => setIsOpen(!isOpen)}>
        {isOpen ? (
          <span style={{ fontSize: '1.5rem', color: 'white', fontWeight: 'bold' }}>✕</span>
        ) : (
          <span style={{ fontSize: '1.8rem' }}>🤖</span>
        )}
      </button>

      {isOpen && (
        <div className="glass-card ai-chat-window">
          <div className="ai-chat-header">
            <div className="ai-chat-title">
              <span style={{ fontSize: '1.3rem' }}>🤖</span>
              <div>
                <h4 style={{ margin: 0, fontSize: '0.95rem' }}>AI Shopping Guide</h4>
                <span style={{ fontSize: '0.7rem', opacity: 0.8 }}>
                  {isBackendConnected ? "Ollama model active" : "Local AI Simulation"}
                </span>
              </div>
            </div>
            <button 
              onClick={() => setIsOpen(false)} 
              style={{ background: 'none', border: 'none', color: 'white', fontSize: '1.2rem', cursor: 'pointer' }}
            >
              ✕
            </button>
          </div>

          <div className="ai-chat-body">
            {messages.map(msg => (
              <div 
                key={msg.id} 
                className={`chat-message ${msg.assistant ? 'assistant' : 'user'}`}
              >
                {msg.text}
              </div>
            ))}
            {isTyping && (
              <div className="chat-message assistant" style={{ fontStyle: 'italic', color: 'var(--text-muted)' }}>
                AI is typing...
              </div>
            )}
            <div ref={chatEndRef} />
          </div>

          <form onSubmit={handleSend} className="ai-chat-footer">
            <input 
              type="text" 
              className="ai-chat-input" 
              placeholder="Ask about products, reviews..." 
              value={input}
              onChange={(e) => setInput(e.target.value)}
            />
            <button type="submit" className="btn-primary" style={{ padding: '0.5rem 1rem' }}>
              Send
            </button>
          </form>
        </div>
      )}
    </>
  );
}
