export type ProblemDataTreeNodeKind = 'file' | 'directory'

export function fromProblemDataTreeNodeKindContract(value: unknown): ProblemDataTreeNodeKind {
  switch (value) {
    case 'file':
    case 'directory':
      return value
    default:
      throw new Error('Invalid problem data tree node kind in contract payload.')
  }
}
