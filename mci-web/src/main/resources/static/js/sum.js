// Streams the column-addition steps from POST /sum/stream (SSE format)
// and renders them live. EventSource only supports GET, so fetch is
// used and the event stream is parsed by hand. Without JS the form
// still submits via POST and the page renders the sum only.

const form = document.getElementById('sum-form');
const emptyState = document.getElementById('empty-state');
const resultPanel = document.getElementById('result-panel');
const resultValue = document.getElementById('result-value');
const stepsPanel = document.getElementById('steps-panel');
const stepsList = document.getElementById('steps-list');
const errorPanel = document.getElementById('error-panel');
const errorValue = document.getElementById('error-value');

let aborter = null;
let resultSoFar = '';

form.addEventListener('submit', (event) => {
    event.preventDefault();
    if (aborter) {
        aborter.abort();
    }
    aborter = new AbortController();
    resultSoFar = '';
    stepsList.replaceChildren();
    stepsPanel.hidden = true;
    resultPanel.hidden = true;
    errorPanel.hidden = true;
    if (emptyState) {
        emptyState.hidden = true;
    }

    streamSum(new FormData(form), aborter.signal);
});

async function streamSum(params, signal) {
    try {
        const response = await fetch('/sum/stream', {
            method: 'POST',
            body: params,
            signal
        });
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        for (;;) {
            const { done, value } = await reader.read();
            if (done) {
                break;
            }
            buffer += decoder.decode(value, { stream: true });
            const events = buffer.split('\n\n');
            buffer = events.pop(); // last chunk may be incomplete
            for (const raw of events) {
                handleEvent(raw);
            }
        }
    } catch (e) {
        if (e.name !== 'AbortError') {
            showError('Connection lost before the calculation finished.');
        }
    }
}

function handleEvent(raw) {
    const name = raw.match(/^event:(\w+)/m)?.[1];
    const data = raw.match(/^data:(.*)$/m)?.[1];
    if (!name || data === undefined) {
        return;
    }
    const payload = JSON.parse(data);
    if (name === 'step') {
        resultSoFar = payload.resultDigit + resultSoFar;
        const li = document.createElement('li');
        li.textContent = `${payload.firstDigit} + ${payload.secondDigit} + carry ${payload.carryIn} = ${payload.columnTotal}. `
            + `Write ${payload.resultDigit}, carry ${payload.carryOut}. Result so far: ${resultSoFar}`;
        stepsList.appendChild(li);
        stepsPanel.hidden = false;
    } else if (name === 'result') {
        resultValue.textContent = payload.sum;
        resultPanel.hidden = false;
    } else if (name === 'error') {
        showError(payload.message);
    }
}

function showError(message) {
    errorValue.textContent = message;
    errorPanel.hidden = false;
}
