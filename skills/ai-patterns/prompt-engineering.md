# Prompt Engineering Patterns

## Prompt Structure

A well-structured prompt has clear layers:

```
1. Role / Persona     — who the model is
2. Context            — what situation it's in
3. Task               — what to do
4. Constraints        — what NOT to do
5. Output format      — how to format the response
6. Examples           — few-shot demonstrations (when needed)
```

## Core Techniques

### Be Specific, Not Verbose
- Bad: "Please analyze this code and tell me if there are any potential issues you can find."
- Good: "Review this Python function for SQL injection vulnerabilities. List each vulnerability with: location, severity (HIGH/MEDIUM/LOW), and a one-line fix."

### System vs. User Prompts
- **System prompt**: persona, behavior rules, output format, constraints (stable across turns)
- **User prompt**: the actual task or question (changes each turn)

### Chain of Thought
For complex reasoning, ask the model to think step-by-step:
```
Before answering, reason through:
1. What is the core question?
2. What information do I have?
3. What assumptions am I making?
Then provide your answer.
```

### Few-Shot Examples
Show the pattern when the output format is non-obvious:
```
Examples:
Input: "the user cant login"
Output: { "category": "auth", "severity": "high", "title": "Login failure" }

Input: "dashboard loads slow"
Output: { "category": "performance", "severity": "medium", "title": "Dashboard latency" }

Now classify: "payment page crashes on mobile"
```

## Output Control

### Structured Output
Force JSON/YAML for programmatic consumption:
```
Respond ONLY with valid JSON matching this schema:
{ "result": string, "confidence": number, "reasoning": string }
Do not include any text before or after the JSON.
```

### Length Control
- "In one sentence:" for concise answers
- "In under 200 words:" for bounded explanations
- "Provide a complete implementation:" for full output

## Agentic Patterns

### Tool Use
Define tools with exact parameter schemas. The model will call them when needed.

### ReAct Pattern (Reason + Act)
```
Thought: [what I'm thinking]
Action: [tool to call] with [args]
Observation: [result]
... repeat until done
Answer: [final response]
```

### Self-Correction
Ask the model to review its own output:
```
After generating your answer, check:
- Does it fully address the question?
- Are there any logical errors?
- Is the format correct?
If any check fails, revise before responding.
```
