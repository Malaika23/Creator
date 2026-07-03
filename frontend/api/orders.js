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
    const { items, totalAmount, shippingAddress } = req.body;

    if (!items || items.length === 0) {
      res.status(400).json({ error: "Missing order items" });
      return;
    }

    // Simulate database record creation
    const createdOrder = {
      id: Math.floor(Math.random() * 900000) + 100000,
      items,
      totalAmount,
      shippingAddress,
      status: "CONFIRMED", // Simulated successful payment flow
      createdAt: new Date().toISOString()
    };

    res.status(201).json(createdOrder);
  } else {
    res.status(405).json({ error: "Method not allowed" });
  }
}
