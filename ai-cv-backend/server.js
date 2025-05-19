const express = require('express');
const bodyParser = require('body-parser');
const axios = require('axios');
require('dotenv').config();

const app = express();
app.use(bodyParser.json());

app.post('/generate-cv', async (req, res) => {
  const profile = req.body.profile;

  const prompt = `
You are a professional resume generator.
Use the following profile to generate a clean, structured CV in JSON format:

${JSON.stringify(profile, null, 2)}

Return only the JSON CV.
`;

  try {
    const response = await axios.post('https://api.cohere.ai/v1/chat', {
      model: "command-r-plus",
      message: prompt,
      temperature: 0.3
    }, {
      headers: {
        'Authorization': `Bearer ${process.env.COHERE_API_KEY}`,
        'Content-Type': 'application/json'
      }
    });

    const generatedText = response.data.text;
    res.json({ cv: generatedText });
  } catch (err) {
    console.error("Cohere Error:", err.response ? err.response.data : err.message);
    res.status(500).json({ error: "CV generation failed." });
  }
});

app.listen(3000, () => console.log('Cohere server running on port 3000'));
