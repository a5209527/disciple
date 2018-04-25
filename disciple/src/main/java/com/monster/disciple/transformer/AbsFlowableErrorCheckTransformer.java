package com.monster.disciple.transformer;

import android.content.Context;
import android.text.TextUtils;

import com.monster.disciple.CacheBuilder;
import com.monster.disciple.entity.CacheEntity;
import com.monster.disciple.response.BaseResponse;
import com.monster.disciple.util.Util;

import org.reactivestreams.Publisher;

import java.lang.ref.WeakReference;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Response;

public abstract class AbsFlowableErrorCheckTransformer<T extends Response<R>, R extends BaseResponse>
        implements FlowableTransformer<T, R>, IErrorCheckTransformer {

    protected WeakReference<Context> mContextRef;
    private CacheBuilder mCacheBuilder;

    public AbsFlowableErrorCheckTransformer(final Context context) {
        mContextRef = new WeakReference<>(context);
    }

    public AbsFlowableErrorCheckTransformer(final Context context, final CacheBuilder cacheBuilder) {
        mContextRef = new WeakReference<>(context);
        mCacheBuilder = cacheBuilder;
    }

    @Override
    public Publisher<R> apply(Flowable<T> upstream) {
        if (mCacheBuilder == null || TextUtils.isEmpty(mCacheBuilder.getCacheKey())) {
            return upstream
                    .map(Util.<T, R>newInstanceFunction(this))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
        } else {
            return upstream
                    .map(Util.<T, R>newInstanceFunction(this))
                    .map(Util.<R>saveCacheFunction(mContextRef.get(), mCacheBuilder.getCacheKey()))
                    .startWith(
                            Flowable
                                    .just(mCacheBuilder.getCacheKey())
                                    .map(Util.<R>getCacheFunction(mContextRef.get(), mCacheBuilder.getType()))
                                    .filter(new Predicate<CacheEntity<R>>() {
                                        @Override
                                        public boolean test(CacheEntity<R> rCacheEntity) {
                                            return !rCacheEntity.isNil();
                                        }
                                    })
                                    .map(new Function<CacheEntity<R>, R>() {
                                        @Override
                                        public R apply(CacheEntity<R> rCacheEntity) {
                                            return rCacheEntity.getResponse();
                                        }
                                    })
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
        }
    }
}
