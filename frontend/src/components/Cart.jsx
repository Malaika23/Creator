import React from 'react';

export default function Cart({ cartItems, onClose, onUpdateQty, onCheckout }) {
  const total = cartItems.reduce((sum, item) => sum + (item.product.price * item.quantity), 0);

  return (
    <div className="overlay-panel">
      <div className="panel-header">
        <h2 style={{ fontWeight: 800 }}>Your Bag</h2>
        <button 
          onClick={onClose} 
          style={{ background: 'none', border: 'none', color: 'var(--text-main)', fontSize: '1.5rem', cursor: 'pointer' }}
        >
          ✕
        </button>
      </div>

      <div className="panel-body">
        {cartItems.length === 0 ? (
          <div style={{ textAlign: 'center', marginTop: '4rem', color: 'var(--text-muted)' }}>
            <span style={{ fontSize: '3rem' }}>🛍️</span>
            <p style={{ marginTop: '1rem', fontSize: '1rem' }}>Your shopping bag is empty.</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            {cartItems.map(item => (
              <div 
                key={item.product.id} 
                style={{ 
                  display: 'flex', 
                  gap: '1rem', 
                  alignItems: 'center', 
                  borderBottom: '1px solid var(--border)',
                  paddingBottom: '1.25rem' 
                }}
              >
                <div style={{ 
                  width: '60px', 
                  height: '60px', 
                  background: 'rgba(255, 255, 255, 0.03)', 
                  border: '1px solid var(--border)',
                  borderRadius: '10px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '1.5rem'
                }}>
                  {item.product.imagePlaceholder || "📦"}
                </div>
                <div style={{ flexGrow: 1 }}>
                  <h4 style={{ fontWeight: 700, fontSize: '0.95rem' }}>{item.product.name}</h4>
                  <span style={{ fontSize: '0.85rem', color: 'var(--accent)', fontWeight: 700 }}>
                    ${item.product.price.toFixed(2)}
                  </span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <button 
                    onClick={() => onUpdateQty(item.product.id, -1)}
                    style={{ 
                      width: '28px', 
                      height: '28px', 
                      borderRadius: '6px', 
                      border: '1px solid var(--border)',
                      background: 'transparent',
                      color: 'var(--text-main)',
                      cursor: 'pointer'
                    }}
                  >
                    -
                  </button>
                  <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>{item.quantity}</span>
                  <button 
                    onClick={() => onUpdateQty(item.product.id, 1)}
                    style={{ 
                      width: '28px', 
                      height: '28px', 
                      borderRadius: '6px', 
                      border: '1px solid var(--border)',
                      background: 'transparent',
                      color: 'var(--text-main)',
                      cursor: 'pointer'
                    }}
                  >
                    +
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {cartItems.length > 0 && (
        <div className="panel-footer">
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1.5rem', alignItems: 'center' }}>
            <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>Total</span>
            <span style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-main)' }}>
              ${total.toFixed(2)}
            </span>
          </div>
          <button 
            className="btn-primary" 
            style={{ width: '100%', justifyContent: 'center', padding: '1rem' }}
            onClick={onCheckout}
          >
            Checkout 🚀
          </button>
        </div>
      )}
    </div>
  );
}
