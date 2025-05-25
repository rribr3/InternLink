const express = require('express');
const bodyParser = require('body-parser');
const axios = require('axios');
require('dotenv').config();

const app = express();
app.use(bodyParser.json());

app.post('/generate-cv', async (req, res) => {
  const profile = req.body.profile;

  const prompt = `
  You are a professional resume writer.
  Using the following profile, write a full, professional, well-formatted CV in natural English text. Include:
  
  - Full name and contact info at the top
  - A short professional summary
  - Education history
  - Work experience (as bullet points)
  - Projects (with descriptions)
  - Skills (bulleted list)
  - Certifications
  - Languages (language name + proficiency level)
  
  Make it sound polished and suitable for job applications in tech:
  
  ${JSON.stringify(profile, null, 2)}
  `;
  


  try {
    const response = await axios.post('https://api.cohere.ai/v1/generate', {
  model: "command",
  prompt: prompt,  // 
  max_tokens: 800,
  temperature: 0.5,
}, {
  headers: {
    'Authorization': `Bearer ${process.env.COHERE_API_KEY}`,
    'Content-Type': 'application/json'
  }
});


    const generatedText = response.data.generations[0].text;
    res.json({ cv: generatedText });
  } catch (err) {
    console.error("Cohere Error:", err.response ? err.response.data : err.message);
    res.status(500).json({ error: "CV generation failed." });
  }
});

app.listen(3000, () => console.log('Cohere server running on port 3000'));
