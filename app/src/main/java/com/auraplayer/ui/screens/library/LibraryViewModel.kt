package com.auraplayer.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.auraplayer.domain.model.Song
import com.auraplayer.domain.repository.SongRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repo: SongRepository
) : ViewModel() {

    companion object {
        fun provideFactory(repo: SongRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LibraryViewModel(repo) as T
            }
    }

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(LibraryFilter.SONGS)
    val filter: StateFlow<LibraryFilter> = _filter.asStateFlow()

    val songs: StateFlow<List<Song>> = _query
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) repo.getAllSongs() else repo.search(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { repo.syncWithMediaStore() }
    }

    fun onQueryChange(q: String) { _query.value = q }
    fun onFilterChange(f: LibraryFilter) { _filter.value = f }
}

enum class LibraryFilter { SONGS, ALBUMS, ARTISTS, PLAYLISTS }
