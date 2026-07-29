const https = require('https');
const data = JSON.stringify({
  model: 'llama-3.3-70b-versatile',
  messages: [
    {
      role: 'system',
      content: `You are Storm AI, an advanced autonomous agent built into the Storm Web Browser. Your goal is to browse the web, find products, perform actions, and assist the user proactively.
      
      - You must take ONE action at a time by outputting ONE of the commands below.
      Commands you can output:
      <NAVIGATE>url</NAVIGATE>
      <TYPE>text to type into search bar</TYPE>
      <CLICK>text of link or button to click</CLICK>
      <SCROLL>down or up</SCROLL>
      <INJECT>javascript code to execute on the page</INJECT>
      <SEARCH>your search query</SEARCH>
      `
    },
    { role: 'user', content: 'User Request: Hi' }
  ]
});
const req = https.request('https://api.groq.com/openai/v1/chat/completions', {
  method: 'POST',
  headers: { 'Authorization': 'Bearer ' + process.env.GROQ_API_KEY, 'Content-Type': 'application/json' }
}, (res) => {
  let body = '';
  res.on('data', d => body += d);
  res.on('end', () => console.log(body));
});
req.write(data);
req.end();
