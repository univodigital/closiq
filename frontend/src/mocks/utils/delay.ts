export async function delay(ms: number): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, ms));
}

export async function simulateError(rate = 0): Promise<void> {
  if (rate > 0 && Math.random() < rate) {
    throw new Error("Simulated network error");
  }
}
