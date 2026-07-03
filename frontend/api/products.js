const MOCK_PRODUCTS = [
  { id: 1, name: "Zephyrus G14 Gaming Laptop", description: "Ultra-slim gaming laptop with AMD Ryzen 9, RTX 4060, and 120Hz ROG Nebula display.", price: 1450.00, category: "Electronics", imagePlaceholder: "💻", stock: 12 },
  { id: 2, name: "AeroSound ANC Headphones", description: "Active noise-cancelling wireless over-ear headphones with hi-res audio and 40h battery.", price: 299.99, category: "Audio", imagePlaceholder: "🎧", stock: 24 },
  { id: 3, name: "OmniFocus Smartwatch 2", description: "Fitness smartwatch with continuous heart-rate tracking, blood oxygen, and built-in GPS.", price: 199.50, category: "Wearables", imagePlaceholder: "⌚", stock: 15 },
  { id: 4, name: "Quantum Pro mechanical Keyboard", description: "RGB hot-swappable mechanical keyboard with custom linear switches and double-shot keycaps.", price: 129.99, category: "Accessories", imagePlaceholder: "⌨️", stock: 30 },
  { id: 5, name: "PixelView 27\" QHD Monitor", description: "27-inch IPS gaming monitor with 170Hz refresh rate, 1ms response, and HDR400.", price: 349.99, category: "Electronics", imagePlaceholder: "🖥️", stock: 8 },
  { id: 6, name: "LunaCharge Wireless Pad", description: "15W fast wireless charging pad with premium aluminum casing and soft fabric top.", price: 39.99, category: "Accessories", imagePlaceholder: "⚡", stock: 50 }
];

export default function handler(req, res) {
  // Set CORS headers for Vercel functions compatibility
  res.setHeader('Access-Control-Allow-Credentials', true);
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS,PATCH,DELETE,POST,PUT');
  res.setHeader(
    'Access-Control-Allow-Headers',
    'X-CSRF-Token, X-Requested-With, Accept, Accept-Version, Content-Length, Content-MD5, Content-Type, Date, X-Api-Version'
  );

  if (req.method === 'OPTIONS') {
    res.status(200).end();
    return;
  }

  const { category, search } = req.query;
  let result = [...MOCK_PRODUCTS];

  if (category && category !== 'All') {
    result = result.filter(p => p.category.toLowerCase() === category.toLowerCase());
  }

  if (search) {
    const term = search.toLowerCase();
    result = result.filter(p => p.name.toLowerCase().includes(term) || p.description.toLowerCase().includes(term));
  }

  res.status(200).json(result);
}
