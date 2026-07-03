const MOCK_PRODUCTS = [
  { id: 1, name: "Zephyrus G14 Gaming Laptop", price: 1450.00 },
  { id: 2, name: "AeroSound ANC Headphones", price: 299.99 },
  { id: 3, name: "OmniFocus Smartwatch 2", price: 199.50 },
  { id: 4, name: "Quantum Pro mechanical Keyboard", price: 129.99 },
  { id: 5, name: "PixelView 27\" QHD Monitor", price: 349.99 },
  { id: 6, name: "LunaCharge Wireless Pad", price: 39.99 }
];

export default function handler(req, res) {
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

  if (req.method === 'POST') {
    const { message } = req.query;
    const text = (message || "").toLowerCase();
    let reply = "";

    if (text.includes("gaming") || text.includes("laptop")) {
      const laptop = MOCK_PRODUCTS.find(p => p.id === 1);
      reply = `I found a great match! The **${laptop.name}** is an ultra-slim gaming laptop powered by AMD Ryzen 9 and an NVIDIA RTX 4060 graphics card, available for **$${laptop.price}**. Would you like me to add it to your cart?`;
    } else if (text.includes("headphone") || text.includes("audio") || text.includes("sound")) {
      const headphones = MOCK_PRODUCTS.find(p => p.id === 2);
      reply = `You might like the **${headphones.name}**! They feature active noise cancellation (ANC), premium Hi-Res audio drivers, and up to 40 hours of battery life. They are priced at **$${headphones.price}**.`;
    } else if (text.includes("watch") || text.includes("fitness") || text.includes("smartwatch")) {
      const watch = MOCK_PRODUCTS.find(p => p.id === 3);
      reply = `Check out the **${watch.name}** for **$${watch.price}**. It comes with continuous heart-rate tracking, blood oxygen levels (SpO2), and integrated GPS mapping.`;
    } else if (text.includes("keyboard") || text.includes("mechanical")) {
      const keyboard = MOCK_PRODUCTS.find(p => p.id === 4);
      reply = `Yes, we have the **${keyboard.name}**. It's hot-swappable with linear switches, vibrant RGB lighting, and dual-shot keycaps for **$${keyboard.price}**.`;
    } else if (text.includes("monitor") || text.includes("screen") || text.includes("display")) {
      const monitor = MOCK_PRODUCTS.find(p => p.id === 5);
      reply = `The **${monitor.name}** is excellent for work and gaming. It has a 27" QHD screen with an ultra-fast 170Hz refresh rate and HDR support, priced at **$${monitor.price}**.`;
    } else if (text.includes("charge") || text.includes("wireless") || text.includes("pad")) {
      const charger = MOCK_PRODUCTS.find(p => p.id === 6);
      reply = `We have the **${charger.name}** for **$${charger.price}**. It supports 15W fast wireless charging for your smartphone or wireless charger pad.`;
    } else if (text.includes("hello") || text.includes("hi") || text.includes("hey")) {
      reply = "Hello! I am your AI assistant. Tell me what product or category you are interested in, and I will recommend the best option!";
    } else {
      reply = "I understand you are looking for shopping help. Try searching for keywords like **gaming laptop**, **headphones**, **wireless charger**, or **mechanical keyboard** to get customized product insights!";
    }

    res.status(200).json({ reply });
  } else {
    res.status(405).json({ error: "Method not allowed" });
  }
}
