const fs = require('fs');
const https = require('https');

const data = JSON.stringify({
  model: 'whisper-large-v3-turbo',
  messages: [{role: 'user', content: 'test'}]
});

const req = https.request('https://api.groq.com/openai/v1/chat/completions', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ' + process.env.GROQ_API_KEY,
    'Content-Type': 'application/json'
  }
}, (res) => {
  let body = '';
  res.on('data', d => body += d);
  res.on('end', () => console.log(res.statusCode, body));
});
req.write(data);
req.end();
