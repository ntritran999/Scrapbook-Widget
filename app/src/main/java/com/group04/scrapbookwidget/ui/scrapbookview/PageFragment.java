package com.group04.scrapbookwidget.ui.scrapbookview;

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

import java.util.List;
import java.util.concurrent.ExecutionException;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PageFragment extends Fragment {
    private ScrapbookViewModel scrapbookViewModel;
    private PageCurlView pageCurlView;

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
        scrapbookViewModel = new ViewModelProvider(requireParentFragment()).get(ScrapbookViewModel.class);
        scrapbookViewModel.getPagesLiveData().observe(getViewLifecycleOwner(), pages -> {
            if (pages != null && !pages.isEmpty()) {
                prepareBitmaps(pages, scrapbookViewModel.getPageIndex());

                pageCurlView.setPhotoRects(pages);
                pageCurlView.setOnPhotoHitListener((pageId, itemId) -> {
                    PhotoDialogFragment photoDialogFragment = PhotoDialogFragment.newInstance(scrapbookViewModel.getGroupId(), pageId, itemId);
                    photoDialogFragment.show(getChildFragmentManager(), PhotoDialogFragment.TAG);
                });
            }
            else {
                Toast.makeText(getContext(), "Cannot load pages.", Toast.LENGTH_SHORT).show();
            }
        });
        scrapbookViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            Toast.makeText(getContext(), "Cannot load pages", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        pageCurlView = new PageCurlView(this.getActivity());
        return pageCurlView;
    }

    private void prepareBitmaps(List<ScrapbookPageData> data, int page) {
        pageCurlView.setCurPage(page);
        new Thread(() -> {
            try {
                PageResources resources = PageBuilder.buildPages(getContext(), data);

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