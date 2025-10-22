"use client"

import { useState } from "react"
import { Card } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Search, Filter, Play, FileText, BookOpen } from "lucide-react"

export default function ResourceLibrary() {
  const [searchQuery, setSearchQuery] = useState("")
  const [selectedType, setSelectedType] = useState("all")

  const resources = [
    { id: 1, title: "Derivatives Explained", type: "video", duration: "12 min", views: 2400, rating: 4.8 },
    { id: 2, title: "Calculus Fundamentals", type: "article", pages: 15, views: 1800, rating: 4.6 },
    { id: 3, title: "Integration Techniques", type: "video", duration: "18 min", views: 1200, rating: 4.9 },
    { id: 4, title: "Practice Problem Set", type: "article", pages: 8, views: 950, rating: 4.7 },
    { id: 5, title: "Chain Rule Mastery", type: "video", duration: "15 min", views: 3100, rating: 4.9 },
    { id: 6, title: "Real-world Applications", type: "article", pages: 12, views: 1100, rating: 4.5 },
  ]

  const filteredResources = resources.filter((r) => {
    const matchesSearch = r.title.toLowerCase().includes(searchQuery.toLowerCase())
    const matchesType = selectedType === "all" || r.type === selectedType
    return matchesSearch && matchesType
  })

  const getTypeIcon = (type: string) => {
    return type === "video" ? <Play size={16} /> : <FileText size={16} />
  }

  const getTypeColor = (type: string) => {
    return type === "video"
      ? "bg-blue-50 text-blue-700 border-l-4 border-l-blue-500"
      : "bg-green-50 text-green-700 border-l-4 border-l-green-500"
  }

  return (
    <div className="p-6 md:p-8 max-w-6xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-foreground mb-2">Resource Library</h1>
        <p className="text-muted-foreground">Explore videos, articles, and practice materials</p>
      </div>

      {/* Search and Filter */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
        <div className="md:col-span-2 relative">
          <Search className="absolute left-3 top-3 text-muted-foreground" size={20} />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search resources..."
            className="w-full pl-10 pr-4 py-2 rounded-lg border border-input bg-background text-foreground placeholder-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary"
          />
        </div>
        <div className="flex gap-2">
          <Filter size={20} className="text-muted-foreground mt-2" />
          <select
            value={selectedType}
            onChange={(e) => setSelectedType(e.target.value)}
            className="flex-1 px-4 py-2 rounded-lg border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option value="all">All Types</option>
            <option value="video">Videos</option>
            <option value="article">Articles</option>
          </select>
        </div>
      </div>

      {/* Resources Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredResources.map((resource) => (
          <Card
            key={resource.id}
            className={`p-4 cursor-pointer transition-all hover:shadow-lg ${getTypeColor(resource.type)}`}
          >
            <div className="flex items-start justify-between mb-3">
              <div className="flex items-center gap-2">
                {getTypeIcon(resource.type)}
                <span className="text-xs font-semibold uppercase opacity-75">{resource.type}</span>
              </div>
              <div className="text-sm font-semibold">★ {resource.rating}</div>
            </div>

            <h3 className="font-semibold text-foreground mb-2">{resource.title}</h3>

            <div className="flex items-center justify-between text-xs opacity-75 mb-4">
              <span>{resource.type === "video" ? `${resource.duration}` : `${resource.pages} pages`}</span>
              <span>{resource.views.toLocaleString()} views</span>
            </div>

            <Button className="w-full bg-primary/20 text-primary hover:bg-primary/30 border border-current">
              {resource.type === "video" ? "Watch" : "Read"}
            </Button>
          </Card>
        ))}
      </div>

      {filteredResources.length === 0 && (
        <div className="text-center py-12">
          <BookOpen size={48} className="mx-auto text-muted-foreground mb-4 opacity-50" />
          <p className="text-muted-foreground">No resources found matching your search</p>
        </div>
      )}
    </div>
  )
}
