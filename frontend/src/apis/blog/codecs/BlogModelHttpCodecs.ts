import type { BlogCommentContent } from '@/objects/blog/BlogCommentContent'
import type { BlogCommentId } from '@/objects/blog/BlogCommentId'
import type { BlogContent } from '@/objects/blog/BlogContent'
import type { BlogId } from '@/objects/blog/BlogId'
import type { BlogProblemReference } from '@/objects/blog/BlogProblemReference'
import type { BlogTitle } from '@/objects/blog/BlogTitle'
import type { BlogVisibility } from '@/objects/blog/BlogVisibility'
import type { BlogVote } from '@/objects/blog/BlogVote'
import {
  blogCommentContentValue,
  blogContentValue,
  blogTitleValue,
  parseBlogCommentContent,
  parseBlogCommentId,
  parseBlogContent,
  parseBlogId,
  parseBlogTitle,
  requireParsed,
} from '@/objects/blog/blog-parsers'
import {
  fromProblemSlugContract,
  fromProblemTitleContract,
} from '@/apis/problem/codecs/ProblemModelHttpCodecs'

export type BlogIdContract = number
export type BlogCommentIdContract = number
export type BlogTitleContract = string
export type BlogContentContract = string
export type BlogCommentContentContract = string
export type BlogVisibilityContract = 'public' | 'private'
export type BlogVoteContract = 'up' | 'down'

export type BlogProblemReferenceContract = {
  slug: string
  title: string
}

export function fromBlogIdContract(value: BlogIdContract, label: string): BlogId {
  return requireParsed(parseBlogId(value), label)
}

export function fromBlogCommentIdContract(value: BlogCommentIdContract, label: string): BlogCommentId {
  return requireParsed(parseBlogCommentId(value), label)
}

export function fromBlogTitleContract(value: BlogTitleContract, label: string): BlogTitle {
  return requireParsed(parseBlogTitle(value), label)
}

export function toBlogTitleContract(value: BlogTitle): BlogTitleContract {
  return blogTitleValue(value)
}

export function fromBlogContentContract(value: BlogContentContract, label: string): BlogContent {
  return requireParsed(parseBlogContent(value), label)
}

export function toBlogContentContract(value: BlogContent): BlogContentContract {
  return blogContentValue(value)
}

export function fromBlogCommentContentContract(
  value: BlogCommentContentContract,
  label: string,
): BlogCommentContent {
  return requireParsed(parseBlogCommentContent(value), label)
}

export function toBlogCommentContentContract(
  value: BlogCommentContent,
): BlogCommentContentContract {
  return blogCommentContentValue(value)
}

export function fromBlogVisibilityContract(value: BlogVisibilityContract): BlogVisibility {
  return value
}

export function toBlogVisibilityContract(value: BlogVisibility): BlogVisibilityContract {
  return value
}

export function fromBlogVoteContract(value: BlogVoteContract): BlogVote {
  return value
}

export function toBlogVoteContract(value: BlogVote): BlogVoteContract {
  return value
}

export function fromBlogProblemReferenceContract(
  problem: BlogProblemReferenceContract,
  index: number,
): BlogProblemReference {
  return {
    slug: fromProblemSlugContract(problem.slug, `blog related problem slug ${index}`),
    title: fromProblemTitleContract(problem.title, `blog related problem title ${index}`),
  }
}
