import React from 'react';

const CATEGORIES = ["All", "Electronics", "Audio", "Wearables", "Accessories"];

export default function ProductCatalog({ 
  products, 
  activeCategory, 
  setActiveCategory, 
  searchQuery, 
  setSearchQuery, 
  onAddToCart 
}) {
  
  // Filter products based on search and active category
  const filteredProducts = products.filter(product => {
    const matchesCategory = activeCategory === "All" || product.category === activeCategory;
    const matchesSearch = product.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          product.description.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  return (
    <>
      <div className="catalog-header">
        <div>
          <h1 style={{ fontSize: '2.5rem', fontWeight: 800, letterSpacing: '-0.5px', marginBottom: '0.5rem' }}>
            Discover Products
          </h1>
          <p style={{ color: 'var(--text-muted)' }}>
            Browse and search premium, high-quality gear for your ecosystem.
          </p>
        </div>
        <input 
          type="text" 
          placeholder="Search products..." 
          className="search-input"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </div>

      <div style={{ marginBottom: '2rem' }}>
        <div className="categories-list">
          {CATEGORIES.map(category => (
            <button
              key={category}
              className={`category-tab ${activeCategory === category ? 'active' : ''}`}
              onClick={() => setActiveCategory(category)}
            >
              {category}
            </button>
          ))}
        </div>
      </div>

      {filteredProducts.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '4rem 2rem', color: 'var(--text-muted)' }}>
          <span style={{ fontSize: '3rem' }}>🔍</span>
          <h3 style={{ marginTop: '1rem', fontWeight: 600 }}>No products found</h3>
          <p style={{ fontSize: '0.9rem' }}>Try refining your search terms or category selection.</p>
        </div>
      ) : (
        <div className="products-grid">
          {filteredProducts.map(product => (
            <div key={product.id} className="glass-card product-card fade-in-up">
              <div className="product-image-container">
                <span className="product-image-placeholder">{product.imagePlaceholder || "📦"}</span>
                <span style={{ 
                  position: 'absolute', 
                  top: '1rem', 
                  right: '1rem', 
                  background: 'rgba(9, 9, 11, 0.6)', 
                  backdropFilter: 'blur(8px)', 
                  border: '1px solid var(--border)',
                  padding: '0.25rem 0.5rem', 
                  borderRadius: '6px', 
                  fontSize: '0.75rem',
                  fontWeight: 600,
                  color: product.stock > 5 ? 'var(--accent)' : '#f59e0b'
                }}>
                  {product.stock > 0 ? `${product.stock} left` : 'Out of Stock'}
                </span>
              </div>
              <div className="product-info">
                <span className="product-category">{product.category}</span>
                <h3 className="product-name">{product.name}</h3>
                <p className="product-description">{product.description}</p>
                <div className="product-footer">
                  <span className="product-price">${product.price.toFixed(2)}</span>
                  <button 
                    className="btn-primary" 
                    style={{ padding: '0.5rem 1rem', borderRadius: 'var(--radius-sm)', fontSize: '0.85rem' }}
                    onClick={() => onAddToCart(product)}
                    disabled={product.stock <= 0}
                  >
                    Add +
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  );
}
