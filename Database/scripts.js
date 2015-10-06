function dropAll() {
    use vkcrawler_test_1
    db.queue.drop();
    db.friends.drop();

    use vkcrawler_persistence
    db.persistent_journal.drop()
    db.journal_index.drop()
    db.persistent_snapshots.drop()
    db.snaps_index.drop()
};

function init() {
    use vkcrawler_test_1
    //db.tasks_frequency.insert({"type":"friends_list", "freq":NumberLong(3*60*60*1000)}); //initially every 3 hours

    db.queue.insert({id:NumberLong(2763114)})
    db.queue.createIndex({"id":1})
    db.queue.createIndex({"friends_list.lastUseDate":1})
    db.queue.createIndex({"wall_posts.lastUseDate":1})

    db.friends.createIndex({"id":1})

    db.wall_posts.createIndex({owner_id:1, date:1})
    //db.wall_posts.createIndex({owner_id:1, date:1, id:1}, {unique:true})
}

function past(m) {
    var dt = new Date();
    return new Date(dt.setMinutes(dt.getMinutes() - m));
}
