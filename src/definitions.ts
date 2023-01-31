export interface MatterPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
