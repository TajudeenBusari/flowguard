package com.tjtechy.workflowservice.exception;


import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class WorkflowGraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

  @Override
  protected @Nullable GraphQLError resolveToSingleError(@NonNull Throwable ex, @NonNull DataFetchingEnvironment env) {

    if (ex instanceof WorkflowNotFoundException) {
      return GraphqlErrorBuilder.newError(env)
              .message(ex.getMessage())
              .errorType(ErrorType.NOT_FOUND)
              .build();
    }

    if (ex instanceof WorkflowInvalidStateException) {
      return GraphqlErrorBuilder.newError(env)
              .message(ex.getMessage())
              .errorType(ErrorType.BAD_REQUEST)
              .build();
    }

    return null;
  }
}
