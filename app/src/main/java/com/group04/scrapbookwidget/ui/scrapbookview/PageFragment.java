package com.group04.scrapbookwidget.ui.scrapbookview;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.group04.scrapbookwidget.ui.pagecurl.PageBuilder;
import com.group04.scrapbookwidget.ui.pagecurl.PageCurlView;
import com.group04.scrapbookwidget.ui.pagecurl.PageResources;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PageFragment extends Fragment {
    private ScrapbookViewModel scrapbookViewModel;
    private PageCurlView pageCurlView;
    private String curGroupId = "test_id";
    private int curPage = 0;

    public PageFragment() {}

    public static PageFragment newInstance(String param1, String param2) {
        PageFragment fragment = new PageFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        scrapbookViewModel = new ViewModelProvider(this).get(ScrapbookViewModel.class);
        scrapbookViewModel.getPagesLiveData().observe(getViewLifecycleOwner(), pages -> {
            if (pages != null && !pages.isEmpty()) {
                prepareBitmaps(pages, curPage);

                pageCurlView.setPhotoRects(pages);
                pageCurlView.setOnPhotoHitListener((pageId, itemId) -> {
                    PhotoDialogFragment photoDialogFragment = PhotoDialogFragment.newInstance(curGroupId, pageId, itemId);
                    photoDialogFragment.show(getChildFragmentManager(), PhotoDialogFragment.TAG);
                });
            }
        });

        scrapbookViewModel.loadScrapbook(curGroupId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        pageCurlView = new PageCurlView(this.getActivity());
        return pageCurlView;
    }

    private void prepareBitmaps(List<ScrapbookPageData> data, int page) {
        new Thread(() -> {
            try {
                PageResources resources = PageBuilder.buildPages(getContext(), data, page);

                pageCurlView.queueEvent(() -> {
                    pageCurlView.getPageRenderer().updatePageResources(resources);
                    pageCurlView.requestRender();
                });
            } catch (ExecutionException | InterruptedException e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Failed to load images", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}