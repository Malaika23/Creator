import React, { useState } from 'react';

export default function Checkout({ total, onBack, onSubmit }) {
  const [form, setForm] = useState({
    name: "",
    email: "",
    address: "",
    city: "",
    zip: "",
    cardNumber: "",
    expiry: "",
    cvv: ""
  });

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handlePlaceOrder = (e) => {
    e.preventDefault();
    if (!form.name || !form.email || !form.address || !form.cardNumber) {
      alert("Please fill in all required fields.");
      return;
    }
    onSubmit(form);
  };

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <button 
        className="btn-secondary" 
        onClick={onBack}
        style={{ marginBottom: '2rem' }}
      >
        ← Back to Catalog
      </button>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 320px', gap: '2rem', alignItems: 'start' }}>
        {/* Form Details */}
        <div className="glass-card" style={{ padding: '2rem' }}>
          <h2 style={{ marginBottom: '1.5rem', fontWeight: 800 }}>Billing & Shipping</h2>
          <form onSubmit={handlePlaceOrder} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.35rem', fontWeight: 600 }}>Full Name *</label>
                <input 
                  type="text" 
                  name="name" 
                  className="search-input" 
                  style={{ minWidth: '100%', width: '100%' }}
                  placeholder="John Doe"
                  value={form.name}
                  onChange={handleChange}
                  required
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.35rem', fontWeight: 600 }}>Email Address *</label>
                <input 
                  type="email" 
                  name="email" 
                  className="search-input" 
                  style={{ minWidth: '100%', width: '100%' }}
                  placeholder="john@example.com"
                  value={form.email}
                  onChange={handleChange}
                  required
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.35rem', fontWeight: 600 }}>Shipping Address *</label>
              <input 
                type="text" 
                name="address" 
                className="search-input" 
                style={{ minWidth: '100%', width: '100%' }}
                placeholder="123 Science Park Drive"
                value={form.address}
                onChange={handleChange}
                required
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.35rem', fontWeight: 600 }}>City</label>
                <input 
                  type="text" 
                  name="city" 
                  className="search-input" 
                  style={{ minWidth: '100%', width: '100%' }}
                  placeholder="San Francisco"
                  value={form.city}
                  onChange={handleChange}
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.35rem', fontWeight: 600 }}>ZIP Code</label>
                <input 
                  type="text" 
                  name="zip" 
                  className="search-input" 
                  style={{ minWidth: '100%', width: '100%' }}
                  placeholder="94107"
                  value={form.zip}
                  onChange={handleChange}
                />
              </div>
            </div>

            <h3 style={{ marginTop: '1rem', borderTop: '1px solid var(--border)', paddingTop: '1.25rem', fontWeight: 700 }}>Payment Method</h3>
            
            <div>
              <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.35rem', fontWeight: 600 }}>Card Number *</label>
              <input 
                type="text" 
                name="cardNumber" 
                className="search-input" 
                style={{ minWidth: '100%', width: '100%' }}
                placeholder="4111 2222 3333 4444"
                value={form.cardNumber}
                onChange={handleChange}
                required
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.35rem', fontWeight: 600 }}>Expiration (MM/YY)</label>
                <input 
                  type="text" 
                  name="expiry" 
                  className="search-input" 
                  style={{ minWidth: '100%', width: '100%' }}
                  placeholder="12/28"
                  value={form.expiry}
                  onChange={handleChange}
                />
              </div>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.35rem', fontWeight: 600 }}>CVV</label>
                <input 
                  type="password" 
                  name="cvv" 
                  className="search-input" 
                  style={{ minWidth: '100%', width: '100%' }}
                  placeholder="***"
                  value={form.cvv}
                  onChange={handleChange}
                />
              </div>
            </div>

            <button 
              type="submit" 
              className="btn-primary" 
              style={{ width: '100%', justifyContent: 'center', marginTop: '1.5rem', padding: '1rem' }}
            >
              Place Order 🛍️
            </button>
          </form>
        </div>

        {/* Summary Details */}
        <div className="glass-card" style={{ padding: '1.5rem' }}>
          <h3 style={{ marginBottom: '1rem', fontWeight: 700 }}>Order Summary</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', fontSize: '0.9rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-muted)' }}>Subtotal</span>
              <span>${total.toFixed(2)}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-muted)' }}>Shipping</span>
              <span style={{ color: 'var(--accent)', fontWeight: 600 }}>FREE</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid var(--border)', paddingTop: '0.75rem', fontSize: '1.1rem', fontWeight: 800 }}>
              <span>Total</span>
              <span>${total.toFixed(2)}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
