package domains.blog.application

import domains.blog.model.{BlogContent, BlogTitle, BlogVisibility}
import domains.blog.model.request.{CreateBlogRequest, UpdateBlogRequest}
import munit.FunSuite

class BlogValidationSuite extends FunSuite:

  test("validateCreate accepts non-empty trimmed title and content") {
    val request = CreateBlogRequest(BlogTitle(" Hello "), BlogContent(" World "), BlogVisibility.Public)

    val result = BlogValidation.validateCreate(request)

    assertEquals(result, Right(request))
  }

  test("validateCreate rejects blank titles") {
    val request = CreateBlogRequest(BlogTitle("   "), BlogContent("content"), BlogVisibility.Public)

    val result = BlogValidation.validateCreate(request)

    assertEquals(result, Left("Blog title is required."))
  }

  test("validateCreate rejects blank content") {
    val request = CreateBlogRequest(BlogTitle("Title"), BlogContent("   "), BlogVisibility.Public)

    val result = BlogValidation.validateCreate(request)

    assertEquals(result, Left("Blog content is required."))
  }

  test("validateUpdate rejects blank content") {
    val request = UpdateBlogRequest(BlogTitle("Title"), BlogContent("   "), BlogVisibility.Private)

    val result = BlogValidation.validateUpdate(request)

    assertEquals(result, Left("Blog content is required."))
  }

  test("validateUpdate rejects blank titles") {
    val request = UpdateBlogRequest(BlogTitle("   "), BlogContent("content"), BlogVisibility.Private)

    val result = BlogValidation.validateUpdate(request)

    assertEquals(result, Left("Blog title is required."))
  }
