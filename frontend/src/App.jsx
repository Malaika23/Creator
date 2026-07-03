import React, { useState, useEffect } from 'react';
import ProductCatalog from './components/ProductCatalog';
import Cart from './components/Cart';
import Checkout from './components/Checkout';
import AiAssistant from './components/AiAssistant';

// Fallback Mock Data in case backend is not running/connected
const MOCK_PRODUCTS = [
  { id: 1, name: "Zephyrus G14 Gaming Laptop", description: "Ultra-slim gaming laptop with AMD Ryzen 9, RTX 4060, and 120Hz ROG Nebula display.", price: 1450.00, category: "Electronics", imagePlaceholder: "💻", stock: 12 },
  { id: 2, name: "AeroSound ANC Headphones", description: "Active noise-cancelling wireless over-ear headphones with hi-res audio and 40h battery.", price: 299.99, category: "Audio", imagePlaceholder: "🎧", stock: 24 },
  { id: 3, name: "OmniFocus Smartwatch 2", description: "Fitness smartwatch with continuous heart-rate tracking, blood oxygen, and built-in GPS.", price: 199.50, category: "Wearables", imagePlaceholder: "⌚", stock: 15 },
  { id: 4, name: "Quantum Pro mechanical Keyboard", description: "RGB hot-swappable mechanical keyboard with custom linear switches and double-shot keycaps.", price: 129.99, category: "Accessories", imagePlaceholder: "⌨️", stock: 30 },
  { id: 5, name: "PixelView 27\" QHD Monitor", description: "27-inch IPS gaming monitor with 170Hz refresh rate, 1ms response, and HDR400.", price: 349.99, category: "Electronics", imagePlaceholder: "🖥️", stock: 8 },
  { id: 6, name: "LunaCharge Wireless Pad", description: "15W fast wireless charging pad with premium aluminum casing and soft fabric top.", price: 39.99, category: "Accessories", imagePlaceholder: "⚡", stock: 50 }
];

export default function App() {
  const [products, setProducts] = useState(MOCK_PRODUCTS);
  const [cart, setCart] = useState([]);
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [checkoutStep, setCheckoutStep] = useState(null); // 'checkout' or null
  const [orders, setOrders] = useState([]);
  const [activeCategory, setActiveCategory] = useState("All");
  const [searchQuery, setSearchQuery] = useState("");
  const [isBackendConnected, setIsBackendConnected] = useState(false);

  const API_URL = "/api";

  // Try to load products from Backend API
  useEffect(() => {
    fetch(`${API_URL}/products`)
      .then(res => {
        if (!res.ok) throw new Error("API not available");
        return res.json();
      })
      .then(data => {
        if (data && data.length > 0) {
          setProducts(data);
        }
        setIsBackendConnected(true);
      })
      .catch(err => {
        console.warn("Using mock products fallback:", err.message);
        setIsBackendConnected(false);
      });
  }, []);

  const addToCart = (product) => {
    setCart(prevCart => {
      const existing = prevCart.find(item => item.product.id === product.id);
      if (existing) {
        return prevCart.map(item => 
          item.product.id === product.id 
            ? { ...item, quantity: item.quantity + 1 } 
            : item
        );
      }
      return [...prevCart, { product, quantity: 1 }];
    });
    setIsCartOpen(true);
  };

  const updateQuantity = (productId, delta) => {
    setCart(prevCart => {
      return prevCart.map(item => {
        if (item.product.id === productId) {
          const newQty = item.quantity + delta;
          return newQty > 0 ? { ...item, quantity: newQty } : null;
        }
        return item;
      }).filter(Boolean);
    });
  };

  const checkoutTotal = cart.reduce((sum, item) => sum + (item.product.price * item.quantity), 0);

  const placeOrder = (billingDetails) => {
    const orderData = {
      items: cart.map(item => ({ productId: item.product.id, quantity: item.quantity, price: item.product.price })),
      totalAmount: checkoutTotal,
      shippingAddress: billingDetails.address,
      status: "CREATED"
    };

    // If backend is active, try to POST to order service via gateway
    if (isBackendConnected) {
      fetch(`${API_URL}/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(orderData)
      })
      .then(res => res.json())
      .then(createdOrder => {
        setOrders(prev => [createdOrder, ...prev]);
        setCart([]);
        setCheckoutStep(null);
        alert(`Order successfully created! Order ID: ${createdOrder.id}`);
      })
      .catch(err => {
        console.error("Order submission failed, simulating order offline:", err);
        simulateOfflineOrder(orderData);
      });
    } else {
      simulateOfflineOrder(orderData);
    }
  };

  const simulateOfflineOrder = (orderData) => {
    const simulatedOrder = {
      id: Math.floor(Math.random() * 900000) + 100000,
      ...orderData,
      status: "CREATED",
      createdAt: new Date().toLocaleDateString()
    };
    setOrders(prev => [simulatedOrder, ...prev]);
    setCart([]);
    setCheckoutStep(null);
    alert(`Order simulated successfully! Order ID: ${simulatedOrder.id} (Offline Mode)`);
  };

  return (
    <div>
      <header>
        <div className="logo">
          <span>🛒</span>
          <span>NEXUS STORE</span>
        </div>
        <div style={{ display: 'flex', gap: '1.5rem', alignItems: 'center' }}>
          <span style={{ 
            fontSize: '0.8rem', 
            padding: '0.3rem 0.6rem', 
            borderRadius: '12px', 
            background: isBackendConnected ? 'rgba(16, 185, 129, 0.15)' : 'rgba(255, 255, 255, 0.05)',
            color: isBackendConnected ? 'var(--accent)' : 'var(--text-muted)',
            border: `1px solid ${isBackendConnected ? 'var(--accent)' : 'var(--border)'}`
          }}>
            {isBackendConnected ? "● Connected to API" : "● Offline Sandbox"}
          </span>
          <button 
            className="btn-secondary" 
            style={{ position: 'relative' }}
            onClick={() => setIsCartOpen(true)}
          >
            Cart 🛍️
            {cart.length > 0 && (
              <span className="cart-badge">{cart.reduce((sum, item) => sum + item.quantity, 0)}</span>
            )}
          </button>
        </div>
      </header>

      <main className="catalog-container fade-in-up">
        {checkoutStep === 'checkout' ? (
          <Checkout 
            total={checkoutTotal} 
            onBack={() => setCheckoutStep(null)} 
            onSubmit={placeOrder} 
          />
        ) : (
          <ProductCatalog 
            products={products}
            activeCategory={activeCategory}
            setActiveCategory={setActiveCategory}
            searchQuery={searchQuery}
            setSearchQuery={setSearchQuery}
            onAddToCart={addToCart}
          />
        )}
      </main>

      {/* Floating AI Shopping Assistant */}
      <AiAssistant products={products} isBackendConnected={isBackendConnected} apiGateway={API_URL} />

      {/* Slide-out Cart */}
      {isCartOpen && (
        <Cart 
          cartItems={cart}
          onClose={() => setIsCartOpen(false)}
          onUpdateQty={updateQuantity}
          onCheckout={() => {
            setIsCartOpen(false);
            setCheckoutStep('checkout');
          }}
        />
      )}

      {orders.length > 0 && (
        <div className="catalog-container fade-in-up" style={{ marginTop: '4rem', borderTop: '1px solid var(--border)', paddingTop: '2rem' }}>
          <h2 style={{ marginBottom: '1.5rem', fontWeight: 800 }}>Recent Orders ({orders.length})</h2>
          {orders.map(order => (
            <div key={order.id} className="glass-card order-summary-card">
              <div>
                <h4 style={{ fontWeight: 700 }}>Order #{order.id}</h4>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Status: <span style={{ color: 'var(--primary)', fontWeight: 600 }}>{order.status}</span></p>
              </div>
              <div style={{ textAlign: 'right' }}>
                <span style={{ fontWeight: 800, fontSize: '1.2rem', color: 'var(--accent)' }}>${order.totalAmount.toFixed(2)}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
