package com.francescocervone.movies.domain.usecases;


import com.francescocervone.movies.domain.MoviesDataSource;
import com.francescocervone.movies.domain.UseCase;
import com.francescocervone.movies.domain.model.Movie;
import com.francescocervone.movies.domain.model.MoviesPage;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;

public class GetCachedMovies extends UseCase<GetCachedMovies.Request, MoviesPage> {

    private final MoviesDataSource mDataSource;

    public GetCachedMovies(Scheduler executionScheduler, Scheduler postExecutionScheduler,
                           MoviesDataSource dataSource) {
        super(executionScheduler, postExecutionScheduler);

        mDataSource = dataSource;
    }

    @Override
    protected Flowable<MoviesPage> observable(final Request params) {
        return Flowable.defer(new Callable<Publisher<List<List<Movie>>>>() {
            @Override
            public Publisher<List<List<Movie>>> call() throws Exception {
                if (params.mQuery.isEmpty()) {
                    return mDataSource.getNowPlayingMoviesCache();
                } else {
                    return mDataSource.getMoviesCache(params.mQuery);
                }
            }
        }).onErrorResumeNext(Flowable.empty()).map(mapListOfPagesToSinglePage());
    }

    protected Function<List<List<Movie>>, MoviesPage> mapListOfPagesToSinglePage() {
        return pages -> {
            List<Movie> movies = new ArrayList<>();
            for (List<Movie> page : pages) {
                movies.addAll(page);
            }
            return new MoviesPage(pages.size(), movies);
        };
    }

    public static class Request {
        private String mQuery;

        public Request() {
            this("");
        }

        public Request(String query) {
            mQuery = query;
        }

        public static Request findAll() {
            return new Request();
        }

        public static Request find(String query) {
            return new Request(query);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Request)) return false;

            Request request = (Request) o;

            return mQuery != null ? mQuery.equals(request.mQuery) : request.mQuery == null;

        }

        @Override
        public int hashCode() {
            return mQuery != null ? mQuery.hashCode() : 0;
        }
    }
}
